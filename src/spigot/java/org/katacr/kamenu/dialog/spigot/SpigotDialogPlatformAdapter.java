package org.katacr.kamenu.dialog.spigot;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.bungeecord.BungeeComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ClickEventCustom;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.dialog.ConfirmationDialog;
import net.md_5.bungee.api.dialog.Dialog;
import net.md_5.bungee.api.dialog.DialogBase;
import net.md_5.bungee.api.dialog.MultiActionDialog;
import net.md_5.bungee.api.dialog.NoticeDialog;
import net.md_5.bungee.api.dialog.action.Action;
import net.md_5.bungee.api.dialog.action.ActionButton;
import net.md_5.bungee.api.dialog.action.CustomClickAction;
import net.md_5.bungee.api.dialog.action.StaticAction;
import net.md_5.bungee.api.dialog.body.DialogBody;
import net.md_5.bungee.api.dialog.body.PlainMessageBody;
import net.md_5.bungee.api.dialog.input.BooleanInput;
import net.md_5.bungee.api.dialog.input.DialogInput;
import net.md_5.bungee.api.dialog.input.InputOption;
import net.md_5.bungee.api.dialog.input.NumberRangeInput;
import net.md_5.bungee.api.dialog.input.SingleOptionInput;
import net.md_5.bungee.api.dialog.input.TextInput;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCustomClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.katacr.kamenu.DialogSessionManager;
import org.katacr.kamenu.InputCaptureUtils;
import org.katacr.kamenu.KaMenu;
import org.katacr.kamenu.MenuActions;
import org.katacr.kamenu.MenuManager;
import org.katacr.kamenu.MenuRequirementChecker;
import org.katacr.kamenu.MenuTaskManager;
import org.katacr.kamenu.TextParser;
import org.katacr.kamenu.dialog.DialogBodyDefinition;
import org.katacr.kamenu.dialog.DialogButtonDefinition;
import org.katacr.kamenu.dialog.DialogDefinition;
import org.katacr.kamenu.dialog.DialogDefinitionCompiler;
import org.katacr.kamenu.dialog.DialogInputDefinition;
import org.katacr.kamenu.dialog.DialogInputSchema;
import org.katacr.kamenu.dialog.DialogOptionDefinition;
import org.katacr.kamenu.dialog.DialogPlatformAdapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 使用 Spigot 1.21.6+ 的 Bungee Dialog API 渲染 KaMenu 的中立 Dialog 定义。
 *
 * 按钮通过一次性 custom-click session 回传输入，随后复用 KaMenu 的通用 actions、Events 与 Tasks 执行器。
 * session 只在服务端保存可信动作路径和玩家身份，客户端提交内容仅作为已声明 input 的候选值。
 */
public final class SpigotDialogPlatformAdapter implements DialogPlatformAdapter, Listener {
    private static final Pattern CLICKABLE_PATTERN = Pattern.compile("(?is)<(text=[^>]*)>");
    private static final int MAX_INPUT_VALUE_LENGTH = 32768;

    private final Map<String, CallbackSession> callbacks = new ConcurrentHashMap<>();
    private final Map<UUID, Set<String>> playerCallbacks = new ConcurrentHashMap<>();
    private final Set<String> reportedItemMappingFailures = ConcurrentHashMap.newKeySet();
    private final SpigotPublicItemMapper itemMapper = new SpigotPublicItemMapper();
    private KaMenu plugin;
    private DialogDefinitionCompiler compiler;

    /** 服务端保存的可信按钮回调上下文。 */
    private record CallbackSession(
            UUID playerId,
            YamlConfiguration config,
            String actionPath,
            String actionReference,
            Map<String, String> initialVariables,
            DialogInputSchema inputSchema,
            List<DialogInputDefinition> inputDefinitions,
            String contextId,
            boolean closesDialogAfterAction,
            long expiresAtMillis
    ) {
    }

    /** 保存一次 Dialog 渲染期间各按钮共同使用的平台中立上下文。 */
    private record RenderContext(
            Player player,
            YamlConfiguration config,
            DialogInputSchema inputSchema,
            List<DialogInputDefinition> inputDefinitions,
            String contextId,
            boolean closesDialogAfterAction
    ) {
    }

    /** Registers the Spigot custom click listener after runtime capability detection. */
    @Override
    public void initialize(KaMenu plugin) {
        this.plugin = plugin;
        this.compiler = new DialogDefinitionCompiler(plugin);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public String getPlatformName() {
        return "Spigot";
    }

    /** Opens a loaded menu on the server thread. */
    @Override
    public void openMenu(Player player, String menuId, MenuManager manager, KaMenu plugin) {
        runOnPrimaryThread(player, () -> {
            YamlConfiguration config = manager.getMenuConfig(menuId);
            if (config == null) {
                player.sendMessage(plugin.getLanguageManager().getMessage("menu.not_found", menuId));
                return;
            }
            openWithLifecycle(player, config, menuId);
        });
    }

    /** Opens an externally supplied in-memory configuration on the server thread. */
    @Override
    public void openConfig(Player player, YamlConfiguration config, KaMenu plugin, String contextId) {
        runOnPrimaryThread(player, () -> openWithLifecycle(player, config, contextId));
    }

    /** 强制打开菜单，不重复执行 Events.Open。 */
    @Override
    public void forceOpenMenu(Player player, String menuId, MenuManager manager, KaMenu plugin) {
        runOnPrimaryThread(player, () -> {
            YamlConfiguration config = manager.getMenuConfig(menuId);
            if (config == null) {
                player.sendMessage(plugin.getLanguageManager().getMessage("menu.not_found", menuId));
                return;
            }
            openDirect(player, config, menuId);
        });
    }

    /** 强制打开外部内存菜单，不重复执行 Events.Open。 */
    @Override
    public void forceOpenConfig(Player player, YamlConfiguration config, KaMenu plugin, String contextId) {
        runOnPrimaryThread(player, () -> openDirect(player, config, contextId));
    }

    /** Clears the native Dialog and cancels its KaMenu lifetime timer. */
    @Override
    public void close(Player player) {
        clearCallbacks(player.getUniqueId());
        player.clearDialog();
    }

    /** 使用 Spigot Bungee Chat API 发送 Adventure 富文本。 */
    @Override
    public void sendMessage(CommandSender sender, Component message) {
        sender.spigot().sendMessage(BungeeComponentSerializer.get().serialize(message));
    }

    /** 使用 Spigot Bungee Chat API 发送 ActionBar。 */
    @Override
    public void sendActionBar(Player player, Component message) {
        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, BungeeComponentSerializer.get().serialize(message));
    }

    /** Spigot 仅暴露字符串标题 API，因此转换为保留颜色的 legacy 文本。 */
    @Override
    public void showTitle(Player player, Component title, Component subtitle, int fadeIn, int stay, int fadeOut) {
        String titleText = BaseComponent.toLegacyText(BungeeComponentSerializer.get().serialize(title));
        String subtitleText = BaseComponent.toLegacyText(BungeeComponentSerializer.get().serialize(subtitle));
        player.sendTitle(titleText, subtitleText, Math.max(0, fadeIn), Math.max(0, stay), Math.max(0, fadeOut));
    }

    /** 发送支持 actions 服务端回调的 Spigot 可点击聊天文本。 */
    @Override
    public void sendClickableText(Player player, String rawText, YamlConfiguration config, String contextId) {
        YamlConfiguration resolvedConfig = config == null ? new YamlConfiguration() : config;
        DialogInputSchema emptySchema = new DialogInputSchema(List.of(), Map.of(), Map.of(), Map.of());
        RenderContext context = new RenderContext(
                player, resolvedConfig, emptySchema, List.of(), contextId == null ? "hovertext" : contextId, false);
        player.spigot().sendMessage(clickableText(context, rawText));
    }

    /** 使用 Spigot 字符串元数据构造可跨平台消费的名称组件。 */
    @Override
    public Component itemName(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null && meta.hasDisplayName()) {
            return LegacyComponentSerializer.legacySection().deserialize(meta.getDisplayName());
        }
        String fallback = java.util.Arrays.stream(item.getType().name().toLowerCase(java.util.Locale.ROOT).split("_"))
                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1))
                .collect(java.util.stream.Collectors.joining(" "));
        return Component.translatable(item.getTranslationKey(), fallback);
    }

    /** 将 Spigot legacy Lore 转换为 Adventure 组件。 */
    @Override
    public List<Component> itemLore(ItemMeta meta) {
        if (!meta.hasLore() || meta.getLore() == null) return List.of();
        return meta.getLore().stream()
                .map(line -> (Component) LegacyComponentSerializer.legacySection().deserialize(line))
                .toList();
    }

    /** 读取 Spigot ItemModel 命名空间键。 */
    @Override
    public String itemModel(ItemMeta meta) {
        return meta.hasItemModel() && meta.getItemModel() != null ? meta.getItemModel().toString() : null;
    }

    /** Releases all pending timeout tasks during plugin shutdown. */
    @Override
    public void shutdown() {
        callbacks.clear();
        playerCallbacks.clear();
        reportedItemMappingFailures.clear();
        plugin = null;
        compiler = null;
    }

    /** 验证并消费一次性按钮 session，然后执行平台中立的 KaMenu 动作列表。 */
    @EventHandler
    public void onCustomClick(PlayerCustomClickEvent event) {
        dispatchCallback(event.getPlayer(), event.getId().toString(), event.getData());
    }

    /** 校验并原子消费玩家绑定的一次性回调，然后执行对应 KaMenu 动作。 */
    private void dispatchCallback(Player player, String callbackId, JsonElement payload) {
        CallbackSession session = callbacks.get(callbackId);
        if (session == null) {
            return;
        }
        if (!session.playerId().equals(player.getUniqueId())) {
            return;
        }
        if (!callbacks.remove(callbackId, session)) {
            return;
        }
        removePlayerCallbackIndex(session.playerId(), callbackId);
        if (session.expiresAtMillis() < System.currentTimeMillis()) return;

        Map<String, String> variables = new LinkedHashMap<>(session.initialVariables());
        variables.putAll(captureInputs(payload, session));
        if (session.closesDialogAfterAction()) {
            clearCallbacks(player.getUniqueId());
        }
        if (session.actionReference() != null) {
            MenuActions.INSTANCE.executeActionReference(
                    player,
                    session.config(),
                    session.actionReference(),
                    variables,
                    session.contextId(),
                    session.closesDialogAfterAction());
        } else {
            MenuActions.INSTANCE.executeConfigActionPath(
                    player,
                    session.config(),
                    session.actionPath(),
                    variables,
                    null,
                    session.closesDialogAfterAction(),
                    session.contextId()
            );
        }
    }

    /** Removes the disconnected player's pending Dialog lifetime task and state. */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        clearCallbacks(event.getPlayer().getUniqueId());
    }

    private void openWithLifecycle(Player player, YamlConfiguration config, String contextId) {
        List<?> openActions = config.getList("Events.Open");
        if (openActions == null || openActions.isEmpty()) {
            openDirect(player, config, contextId);
            return;
        }
        MenuActions.INSTANCE.executeEvent(player, config, "Open", contextId).whenComplete((shouldStop, error) -> {
            if (error != null) {
                plugin.getLogger().severe(plugin.getLanguageManager().getMessage(
                        "spigot_dialog.open_event_failed", contextId, String.valueOf(error.getMessage())));
                error.printStackTrace();
                return;
            }
            if (!Boolean.TRUE.equals(shouldStop)) {
                runOnPrimaryThread(player, () -> openDirect(player, config, contextId));
            }
        });
    }

    private void openDirect(Player player, YamlConfiguration config, String contextId) {
        clearCallbacks(player.getUniqueId());
        if (!MenuRequirementChecker.INSTANCE.check(player, config, plugin)) {
            return;
        }
        DialogDefinition definition;
        try {
            definition = compiler.compile(player, config, contextId);
        } catch (RuntimeException exception) {
            plugin.getLogger().warning(plugin.getLanguageManager().getMessage(
                    "spigot_dialog.compile_failed", contextId, String.valueOf(exception.getMessage())));
            exception.printStackTrace();
            return;
        }

        try {
            Dialog dialog = dialog(player, config, definition, contextId);
            player.showDialog(dialog);
            DialogSessionManager.INSTANCE.attach(player, config, contextId);
            MenuTaskManager.INSTANCE.attachMenu(player, config, contextId);
        } catch (RuntimeException exception) {
            clearCallbacks(player.getUniqueId());
            plugin.getLogger().warning(plugin.getLanguageManager().getMessage(
                    "spigot_dialog.render_failed", contextId, String.valueOf(exception.getMessage())));
            exception.printStackTrace();
        }
    }

    private Dialog dialog(Player player, YamlConfiguration config, DialogDefinition definition, String contextId) {
        RenderContext context = new RenderContext(
                player,
                config,
                definition.getInputSchema(),
                definition.getInputs(),
                contextId,
                definition.getSettings().getAfterAction() == org.katacr.kamenu.dialog.DialogAfterAction.CLOSE
        );
        DialogBase base = new DialogBase(text(player, definition.getTitle()))
                .body(body(context, definition.getBody()))
                .inputs(inputs(player, definition.getInputs()))
                .canCloseWithEscape(definition.getSettings().getCanEscape())
                .pause(definition.getSettings().getPause())
                .afterAction(DialogBase.AfterAction.valueOf(definition.getSettings().getAfterAction().name()));
        return switch (definition.getBottom().getType()) {
            case NOTICE -> notice(context, base, definition.getBottom().getButtons());
            case CONFIRMATION -> confirmation(context, base, definition.getBottom().getButtons());
            case MULTI -> multi(context, base, definition);
        };
    }

    private NoticeDialog notice(RenderContext context, DialogBase base, List<DialogButtonDefinition> definitions) {
        if (definitions.isEmpty()) {
            return new NoticeDialog(base);
        }
        return new NoticeDialog(base, button(context, definitions.get(0)));
    }

    private ConfirmationDialog confirmation(RenderContext context, DialogBase base, List<DialogButtonDefinition> definitions) {
        DialogButtonDefinition yes = definitions.isEmpty()
                ? new DialogButtonDefinition("确认", null, null, "", Map.of()) : definitions.get(0);
        DialogButtonDefinition no = definitions.size() < 2
                ? new DialogButtonDefinition("取消", null, null, "", Map.of()) : definitions.get(1);
        return new ConfirmationDialog(base, button(context, yes), button(context, no));
    }

    private Dialog multi(RenderContext context, DialogBase base, DialogDefinition definition) {
        List<ActionButton> buttons = definition.getBottom().getButtons().stream()
                .map(button -> button(context, button))
                .toList();
        if (buttons.isEmpty()) {
            return new NoticeDialog(base);
        }
        ActionButton exit = definition.getBottom().getExit() == null
                ? null : button(context, definition.getBottom().getExit());
        return new MultiActionDialog(base, buttons, definition.getBottom().getColumns(), exit);
    }

    private List<DialogBody> body(RenderContext context, List<DialogBodyDefinition> definitions) {
        List<DialogBody> body = new ArrayList<>();
        for (DialogBodyDefinition definition : definitions) {
            if (definition instanceof DialogBodyDefinition.Message message) {
                body.add(new PlainMessageBody(clickableText(context, message.getText()), message.getWidth()));
            } else if (definition instanceof DialogBodyDefinition.Item item) {
                PlainMessageBody description = item.getDescription() == null ? null
                        : new PlainMessageBody(clickableText(context, item.getDescription()), item.getDescriptionWidth());
                JsonObject encodedItem = mapItem(context, item);
                body.add(new SpigotItemDialogBody(encodedItem, description,
                        item.getShowOverlays(), item.getShowTooltip(), item.getWidth(), item.getHeight()));
            }
        }
        return List.copyOf(body);
    }

    /** 使用 Bukkit 公共 API 映射物品；单个属性异常时回退到基础物品。 */
    private JsonObject mapItem(RenderContext context, DialogBodyDefinition.Item definition) {
        ItemStack item = definition.getItemStack();
        try {
            return itemMapper.map(item);
        } catch (RuntimeException error) {
            warnItemMappingOnce(context.contextId(), definition.getComponentId(), describeError(error));
        }
        return SpigotItemDialogBody.basicItem(item.getType().getKey().toString(), item.getAmount());
    }

    /** 同一菜单组件的公共属性映射错误只记录一次，避免周期刷新刷屏。 */
    private void warnItemMappingOnce(String contextId, String componentId, String error) {
        if (reportedItemMappingFailures.add(contextId + ':' + componentId)) {
            plugin.getLogger().warning(plugin.getLanguageManager().getMessage(
                    "spigot_dialog.item_mapping_failed", contextId, componentId,
                    error == null ? "unknown error" : error));
        }
    }

    /** 展开包装异常，给管理员保留真正的属性映射失败原因。 */
    private String describeError(Throwable error) {
        Throwable cause = error;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause.getClass().getSimpleName() + ": " +
                (cause.getMessage() == null ? "no message" : cause.getMessage());
    }

    private List<DialogInput> inputs(Player player, List<DialogInputDefinition> definitions) {
        List<DialogInput> inputs = new ArrayList<>();
        for (DialogInputDefinition definition : definitions) {
            if (definition instanceof DialogInputDefinition.Text input) {
                TextInput value = new TextInput(input.getKey(), input.getWidth(), text(player, input.getLabel()),
                        input.getLabelVisible(), input.getInitial(), input.getMaxLength());
                if (input.getMultiline() != null) {
                    value.multiline(new TextInput.Multiline(input.getMultiline().getMaxLines(), input.getMultiline().getHeight()));
                }
                inputs.add(value);
            } else if (definition instanceof DialogInputDefinition.Slider input) {
                inputs.add(new NumberRangeInput(input.getKey(), input.getWidth(), text(player, input.getLabel()),
                        input.getFormat(), input.getMinimum(), input.getMaximum(), input.getStep(), input.getInitial()));
            } else if (definition instanceof DialogInputDefinition.Checkbox input) {
                inputs.add(new BooleanInput(input.getKey(), text(player, input.getLabel()), input.getInitial(),
                        input.getOnTrue(), input.getOnFalse()));
            } else if (definition instanceof DialogInputDefinition.Dropdown input) {
                List<InputOption> options = input.getOptions().stream()
                        .map(option -> option(player, option))
                        .toList();
                inputs.add(new SingleOptionInput(input.getKey(), input.getWidth(), text(player, input.getLabel()),
                        input.getLabelVisible(), options));
            }
        }
        return List.copyOf(inputs);
    }

    private InputOption option(Player player, DialogOptionDefinition option) {
        return new InputOption(option.getId(), text(player, option.getDisplay()), option.getInitial());
    }

    private ActionButton button(RenderContext context, DialogButtonDefinition definition) {
        BaseComponent tooltip = definition.getTooltip() == null ? null : text(context.player(), definition.getTooltip());
        return new ActionButton(text(context.player(), definition.getText()), tooltip, definition.getWidth(),
                action(context, definition));
    }

    /** 保留纯客户端动作，其余按钮注册服务端一次性 callback。 */
    private Action action(RenderContext context, DialogButtonDefinition definition) {
        String path = definition.getActionPath();
        List<?> actions = path.isBlank() ? List.of() : context.config().getList(path);
        if (actions != null && actions.size() == 1 && actions.get(0) instanceof String value) {
            String trimmed = value.trim();
            if (trimmed.startsWith("url:")) {
                return new StaticAction(new ClickEvent(ClickEvent.Action.OPEN_URL, trimmed.substring(4).trim()));
            }
            if (trimmed.startsWith("copy:")) {
                return new StaticAction(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, trimmed.substring(5).trim()));
            }
        }
        String callbackId = registerCallback(
                context, path, null, definition.getVariables(), context.inputSchema(), context.inputDefinitions());
        return new CustomClickAction(callbackId);
    }

    /** 解析正文可点击文本，并将 actions 引用绑定到服务端一次性 callback。 */
    private BaseComponent clickableText(RenderContext context, String rawText) {
        Matcher matcher = CLICKABLE_PATTERN.matcher(rawText);
        if (!matcher.find()) {
            return text(context.player(), rawText);
        }
        TextComponent result = new TextComponent();
        int cursor = 0;
        do {
            result.addExtra(text(context.player(), rawText.substring(cursor, matcher.start())));
            Map<String, String> attributes = attributes(matcher.group(1));
            String label = attributes.getOrDefault("text", "");
            BaseComponent clickable = text(context.player(), label);
            if (attributes.containsKey("hover")) {
                clickable.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                        new BaseComponent[]{text(context.player(), attributes.get("hover"))}));
            }
            if (attributes.containsKey("hover_item") || attributes.containsKey("hover-item")) {
                String source = attributes.getOrDefault("hover_item", attributes.get("hover-item"));
                ItemStack hoverItem = MenuActions.INSTANCE.resolveHoverItem(context.player(), source);
                if (hoverItem != null) {
                    clickable.setHoverEvent(new HoverEvent(
                            HoverEvent.Action.SHOW_ITEM,
                            SpigotHoverItemContent.from(itemMapper.map(hoverItem))));
                }
            }
            if (attributes.containsKey("actions")) {
                DialogInputSchema emptySchema = new DialogInputSchema(List.of(), Map.of(), Map.of(), Map.of());
                String callbackId = registerCallback(
                        context, null, attributes.get("actions"), Map.of(), emptySchema, List.of());
                clickable.setClickEvent(new ClickEventCustom(callbackId, ""));
            } else if (attributes.containsKey("copy")) {
                clickable.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, attributes.get("copy")));
            } else if (attributes.containsKey("command")) {
                clickable.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                        commandValue(attributes.get("command"))));
            } else if (attributes.containsKey("url")) {
                clickable.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, attributes.get("url")));
            }
            result.addExtra(clickable);
            if (Boolean.parseBoolean(attributes.getOrDefault("newline", "false"))) {
                result.addExtra(new TextComponent("\n"));
            }
            cursor = matcher.end();
        } while (matcher.find());
        result.addExtra(text(context.player(), rawText.substring(cursor)));
        return result;
    }

    private Map<String, String> attributes(String content) {
        Map<String, String> attributes = new java.util.LinkedHashMap<>();
        for (String part : content.split(";")) {
            int equals = part.indexOf('=');
            if (equals < 0) continue;
            String key = part.substring(0, equals).trim().toLowerCase(java.util.Locale.ROOT);
            String value = part.substring(equals + 1).trim();
            if ((value.startsWith("`") && value.endsWith("`"))
                    || (value.startsWith("'") && value.endsWith("'"))
                    || (value.startsWith("\"") && value.endsWith("\""))) {
                value = value.substring(1, value.length() - 1);
            }
            attributes.put(key, value);
        }
        return attributes;
    }

    private String commandValue(String command) {
        return command.startsWith("/") ? command : "/" + command;
    }

    private BaseComponent text(Player player, String value) {
        Component parsed = TextParser.INSTANCE.parseText(value, player);
        return new TextComponent(BungeeComponentSerializer.get().serialize(parsed));
    }

    /** 注册一个仅允许指定玩家在当前菜单有效期内消费一次的可信回调。 */
    private String registerCallback(RenderContext context,
                                    String actionPath,
                                    String actionReference,
                                    Map<String, String> variables,
                                    DialogInputSchema inputSchema,
                                    List<DialogInputDefinition> inputDefinitions) {
        String key = "kamenu:dialog_" + UUID.randomUUID().toString().replace("-", "");
        CallbackSession session = new CallbackSession(
                context.player().getUniqueId(),
                context.config(),
                actionPath,
                actionReference,
                Map.copyOf(variables),
                inputSchema,
                List.copyOf(inputDefinitions),
                context.contextId(),
                context.closesDialogAfterAction(),
                System.currentTimeMillis() + DialogSessionManager.INSTANCE.lifetimeSeconds(context.config()) * 1000L
        );
        callbacks.put(key, session);
        playerCallbacks.computeIfAbsent(context.player().getUniqueId(), ignored -> ConcurrentHashMap.newKeySet()).add(key);
        return key;
    }

    /** 从玩家索引移除已消费或已失效的回调。 */
    private void removePlayerCallbackIndex(UUID playerId, String key) {
        Set<String> keys = playerCallbacks.get(playerId);
        if (keys != null) {
            keys.remove(key);
            if (keys.isEmpty()) {
                playerCallbacks.remove(playerId, keys);
            }
        }
    }

    /** 清理玩家当前 Dialog 注册的全部未消费回调。 */
    private void clearCallbacks(UUID playerId) {
        Set<String> keys = playerCallbacks.remove(playerId);
        if (keys != null) {
            keys.forEach(callbacks::remove);
        }
    }

    /** 从不可信 JSON 中只提取并校验当前 Dialog 声明过的输入字段。 */
    private Map<String, String> captureInputs(JsonElement payload, CallbackSession session) {
        JsonObject object = payload != null && payload.isJsonObject() ? payload.getAsJsonObject() : new JsonObject();
        Map<String, DialogInputDefinition> definitions = new LinkedHashMap<>();
        for (DialogInputDefinition definition : session.inputDefinitions()) {
            definitions.put(definition.getKey(), definition);
        }

        Map<String, String> rawValues = new LinkedHashMap<>();
        for (String key : session.inputSchema().getKeys()) {
            JsonElement element = object.get(key);
            if (element == null || !element.isJsonPrimitive()) continue;
            String value = validateInput(element.getAsString(), definitions.get(key));
            if (value != null) {
                rawValues.put(key, value);
            }
        }
        InputCaptureUtils.Schema schema = new InputCaptureUtils.Schema(
                session.inputSchema().getKeys(),
                session.inputSchema().getTypes(),
                session.inputSchema().getRemoveChars(),
                session.inputSchema().getCheckboxMappings()
        );
        return InputCaptureUtils.INSTANCE.captureVariables(plugin, rawValues, schema);
    }

    /** 按输入组件定义限制长度、范围和可选值，拒绝伪造值。 */
    private String validateInput(String value, DialogInputDefinition definition) {
        if (value.length() > MAX_INPUT_VALUE_LENGTH || definition == null) return null;
        if (definition instanceof DialogInputDefinition.Text input) {
            return value.length() <= input.getMaxLength() ? value : value.substring(0, input.getMaxLength());
        }
        if (definition instanceof DialogInputDefinition.Slider input) {
            try {
                double number = Double.parseDouble(value);
                if (!Double.isFinite(number) || number < input.getMinimum() || number > input.getMaximum()) return null;
                return value;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        if (definition instanceof DialogInputDefinition.Dropdown input) {
            return input.getOptions().stream().anyMatch(option -> option.getId().equals(value)) ? value : null;
        }
        if (definition instanceof DialogInputDefinition.Checkbox input) {
            return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")
                    || value.equals("1") || value.equals("0")
                    || value.equalsIgnoreCase("yes") || value.equalsIgnoreCase("no")
                    || value.equals(input.getOnTrue()) || value.equals(input.getOnFalse()) ? value : null;
        }
        return null;
    }

    private void runOnPrimaryThread(Player player, Runnable task) {
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(plugin, task);
        }
    }
}
