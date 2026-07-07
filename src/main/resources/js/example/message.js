// KaMenu built-in global JavaScript package example.
// KaMenu 内置全局 JavaScript 包示例。
//
// Runtime path: plugins/KaMenu/js/example/message.js
// 运行时路径：plugins/KaMenu/js/example/message.js
//
// Package ID: example/message
// 包 ID：example/message
//
// Usage:
// 用法：
//   js: [example/message],Steve
//   text: '{js:[example/message],Steve}'

// args is the argument array passed after the package name.
// args 是包名后方传入的参数数组。
var target = args[0] || name;

// The last expression is returned to {js:...}; js: actions may ignore it.
// 最后一行表达式会返回给 {js:...}；js: 动作可以不使用返回值。
"Hello " + target + " from KaMenu JavaScript package";
