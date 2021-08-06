# Inventory Hotswap
Inventory Hotswap is a client side mod that allows the users to swap their selected item with the items in the column above it.

Simply hold the keybind (Default: Left Alt) and use your scroll wheel to select the item you'd like to swap. Since this mod alters the default GUI overlay there are some config options to allow users to customize their GUI when the keybind is pressed.

 

Quick selection allows users to use the number keys 1-3 while holding the keybind to instantly switch to that item.

Quick selection choices can be inverted through the config option "inverted" which is false by default.

Users can swap an entire row of their inventory at a time, this is done while pressing the keybind (Default: ALT) and sneaking. Quick selection works for this as-well.

![](https://i.imgur.com/20DAvUI.png?raw=true) 



 

 

To edit how the selection bar is displayed go to .minecraft/config/inventoryhotswap-client.toml and choose one of the following options: 

Note: Fabric users must must edit the file at .minecraft/config/inventoryhotswap.json

 

PUSHED (Default) ![](https://i.imgur.com/Bc0o93j.png?raw=true)

  Pushed will move the rest of the survival/adventure overlay up to the top of the vertical selection bar. This is the default mode.



 

INVISIBLE ![](https://i.imgur.com/N51E1vT.png?raw=true)

Invisible will make the survival/adventure overlay invisible only when the keybind is being pressed and will return to normal after a selection is made.



 

OVERLAY ![](https://i.imgur.com/KS8H9xM.png?raw=true)

Overlay does not change the survival/adventure overlay at all and the vertical selection bar is rendered on top of the gui.



 

 

 

Currently Supported Versions:

Forge: 1.15.2 - 1.17.1

    - Jar labeled 1.16 will work for versions 1.16.1 - 1.16.5

Fabric 1.16.1 - 1.167.1

    - Jar labeled 1.16 will work for versions 1.16.1 - 1.16.5
