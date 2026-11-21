// wangeditor 的类型没通过 exports 暴露，这里补模块声明
declare module "@wangeditor/editor-for-vue" {
    import type { Component } from "vue";
    import type { IDomEditor, IEditorConfig, IToolbarConfig } from "@wangeditor/editor";

    export const Editor: Component;
    export const Toolbar: Component;
}
