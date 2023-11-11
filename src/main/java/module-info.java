module FontEditor.main {

    requires dk.martinu.kofi;
    requires dk.martinu.ao.client;
    requires org.jetbrains.annotations;
    requires java.desktop;

    exports dk.martinu.ao.fonteditor;
    exports dk.martinu.ao.fonteditor.util;
    exports dk.martinu.ao.fonteditor.swing;
    exports dk.martinu.ao.fonteditor.edit;
}