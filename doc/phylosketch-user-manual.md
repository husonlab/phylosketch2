# PhyloSketch App User Manual

Daniel H. Huson, University of Tübingen, 2024

## Introduction

PhyloSketch App is an application for interactively creating and editing phylogenetic trees and networks by
drawing them.
The program runs on MacOS X, Linux and Windows, and is also designed for touch-screen devices running iOS or Android.

![example.png](figs/example.png)

## Installation

* The MacOS X, Linux and Windows are available from the GitHub page: https://github.com/husonlab/phylosketch2
* The iOS App is available for testing via Apple TestFlight. If you are interested in
  beta testing, then please contact the author for an invitation.
  ~~* Download and install the PhyloSketch app from the App Store or Google Play.~~
* Open the app and grant any necessary permissions for accessing storage, if prompted.

## Getting Started

When you first open PhyloSketch, you’ll be presented with a canvas containing a simple example tree.
Modify the example or start creating your own phylogenetic tree or network.
The toolbar at the top provides access to all major functions, including mode selection,
import options, and various editing tools.

## Modes Overview

PhyloSketch operates in three primary modes:

### ![edit-mode](figs/edit-mode.png) Edit Mode

* In Edit Mode, you can create new nodes and edges by drawing directly on the canvas.
* Use this mode for building a new tree or network from scratch, or for modifying one.

### ![transform-mode](figs/transform-mode.png) Transform Mode

* Transform Mode allows you to move nodes, reshape edges, and adjust the layout of your phylogenetic tree or network.
* Touch and drag nodes or edges to reposition them.
* Use this mode for fine-tuning the structure after nodes and edges have been created.

### ![read-only-mode](figs/read-only-mode.png) Read-Only Mode

* In Read-Only Mode, editing is locked, but you can still select nodes or edges for viewing details.
* This mode is useful when you want to view a tree or network without the risk of accidentally modifying it.

## Tool Bar Overview

The toolbar provides access to the core functionalities of PhyloSketch:

![toolbar](figs/toolbar.png)

### Mode Selection

• Select the desired mode (Edit, Transform, or Read-Only) using the first item on the tool bar.

### Import Button

* Use the Import button to paste a tree or network (given Newick format) onto the canvas.
* Imported structures will be displayed on the canvas, ready for further editing or analysis.

### Files Menu Button (Desktop version only)

The gives access to file related menu items, including creating a new file,
opening a file and opening a recent file.

### Selection Menu Button

The Selection Menu many items for selecting nodes and/or edges based on their properties.
They are:

* All - select all nodes and edges
* None - deselect all nodes and edges
* Invert - invert the selection
* Extend - extend the selection, by first selecting everything below, then everything above, then everthing
  in the same selected component, and then everything.
* Tree Edges - select all tree edges
* Reticulate Edges - select all reticulate edges
* Roots - select all root nodes (indegree 0)
* Leaves - select all leaves (outdegree 0)
* Tree Nodes - select all tree nodes (indegree 0 or 1)
* Reticulate Nodes - select all reticulate nodes (indegree 2 or more)
* Thru Nodes - select all nodes with indegree 1 and outdegree 1
* Articulation Nodes - nodes that separate biconnected components (using Tarnjan's algorithm)
* Completely Stable Nodes - select all nodes that are on all paths from the root to leaves below them
* Visible Nodes - select all visible nodes, i.e. nodes that have path of tree edges down to a leaf
* Visible Reticulations - select all visible reticulate nodes
* All Below - select all nodes below the currently selected nodes
* All Above - select all nodes above the currently selected nodes
* Possible Root Locations - select all nodes or edges where the root may be placed
* Lowest Stable Ancestor - select the lowest stable ancestor of each selected node
* From Previous Window - select nodes that have the same labels as previously selected nodes


### Layout Menu Button

The Layout Menu enables you to customize how edges are displayed, and allows you to change the layout.
Items apply to the current selection, or to everything, if nothing is selected.

Items for changing how edges appear:

* Outlines - draw tree or network as "outline".

Items for changing the layout:

* Rotate Left - rotate 90o to the left.
* Rotate Right - rotate 90o to the right.
* Flip Horizontal - flip horizontally.
* Flip Vertical - flip vertically.

* Resize Mode - turn resize mode on or off - in resize mode, you can resize and reposition a set of selected nodes.

Other items for modifying the tree or network:

* Remove Thru Nodes - replace any "thru node" (a node that has indegree 1 and outdegree 1) by an edge.

* Set Root - all the root of the tree or network to be changed by selecting a new node or edge.

### Formatting Menu Button

The formatting menu button opens the formatting pane (see below).


### Other Toolbar Buttons

* Delete - remove currently selected nodes and/or edges.
* Undo and redo - Revert or repeat your last actions.
* Zoom In and zoom out - Adjust the view of the canvas for closer inspection or an overview.
* Find - Open or close the find toolbar for locating specific nodes and find/replace toolbar for changing node labels.
* Extend Selection - Expand your current selection to include more nodes or edges.
* Export Menu - Copy the current selection or capture an image of the canvas for sharing.

## Status Bar

The status bar at the bottom of the window reports the number of connected components `comps`, the number
of root nodes `roots`, `nodes`, `edges` and `leaves`. It reports the hybridization number `h` and properties that the
tree or network might have, such as `network`, `tree-based network`, or `temporal network`.

![statusbar](figs/statusbar.png)

## Formatting pane

The formatting pane provides items for styling nodes and edges,
labeling nodes and edges, and setting the styling the node and edge
labels.

### Node style

The node style pane can be used to set the shape, size and color of nodes.

### Node labels

The node labels pane can be used to systematically label nodes A, B, C and some other ways.
In addition, labels can be typed.

### Node labels style

The node labels style pane can be used to set the font, size, style foreground
and background colors of node labels.

### Edge style

The edge style pane can be used to set the shape of edges, to smooth edges, set the
line style (solid, dotted or dashed), width, color, and whether to show arrow heads.

### Edge labels

The edge labels pane can be used to set the weights, support and probability values
of edges. In addition, there is a button for setting the weights of edges based
on their horizontal or vertical distance.
In addition, there are buttons for turning the visualizations of weights, support values and probabilities
on and off.

### Edge labels style

The edge labels style pane can be used to set the font, size, style foreground
and background colors of edge labels.

## Working with Nodes and Edges

### Creating Nodes and Edges

* In Edit Mode, drag along the canvas to create an edge.
* To create a new node, double-click on the canvas or use the context menu item "new node".
* To create new edge, press and drag from a node or edge.
* If you press on an existing node, then this will be the source of the new edge.
* If you press on an existing edge, then a node will be inserted into the existing edge
  and the new edge will attach to it.
* Similarly, if the new edge ends near an existing node or edge, then it will attach to that.

### Transforming Nodes and Edges

* In Transform Mode, select and drag nodes to move them.
* Adjust the shape of edges by dragging on them. After reshaping an edge, optionally use the smooth item to improve
  its shape.
* Fine-tune the layout to achieve the desired appearance.

### Editing Properties

* Use the toolbar to set weights, confidence values, or probabilities for selected edges.
* Toggle the display of these properties using options in the Label Menu.

### Styling Node Labels

Node labels can be styled using a non-standard set of HTML-like tags.
Select the node to be styled and use the context menu to edit the label.
Here are the supported tags:

- `<i>text</i>`: Text in italics
- `<b>text</b>`: Text in bold
- `<sup>text</sup>`: Text as superscript
- `<sub>text</sub>`: Text as subscript
- `<u>text</u>`: Text underlined
- `<a>text</a>`: Text with strike-through
- `<br>`: Start a new line
- `<font "font-name">text</font>`: Text with a specific font
- `<size "font-size">text</size>`: Text with a specific font size
- `<c "font-color">text</c>`: Text with a specific font color
- `<bg "bg-color">text</bg>`: Text with a specific background color
- `<mark shape="value" width="value" height="value" fill="color" stroke="color">`: Adds a mark. Possible marks
  are `Square`, `Circle`, `TriangleUp`, `TriangleDown`, `Diamond`, `Hexagon`, `Rectangle`, `Oval` and `None`.
- `<img src="url" alt="text" width="value" height="value">`: Adds an image.
### Exporting and Sharing

* Use the Export Menu to copy the current selection or save a snapshot of the canvas.
* Exported images or data can be shared directly from your device.
* Displays the Newick description of the current tree or network(s).
* Display a QR code containing the tree or network in Newick format.

## Left-side Tabs (mobile app only)

The mobile app provides tabs on the left-hand-side of the window.

* The Files tab provides a table of all known documents, specifying name, date last opened, size and kind.
* The File tab has a toolbar that provides items to add a new document, duplicate a document, edit the name
  of an existing document or delete a document. There is also a button
  for toggling between light and dark mode.
* You can switch between open documents by clicking on their tabs.

## Menus (non-mobile app only)

The non-mobile version of the PhyloSketch app provides a menu bar with the following menus:

### File menu

* New - open a new document.
* Open - open an existing document or a Newick file.
* Recent - open a recently opened document.
* Export - export a tree or network in Newick format.
* Save - save to a document.
* Page Setup - set up the page for printing.
* Print - print.
* Close - close the current window.
* Quit - quit the program.

### Edit menu

* Undo - undo last operation.
* Redo - redo last operation.
* Cut - cut.
* Сору - copy selected node labels or Newick format.
* Copy Image - copy image of canvas.
* Paste - paste in Newick format.
* Delete - delete selected nodes and/or edges.
* Clear - delete everything.
* Remove Thru Nodes - replace thru nodes by edges.
* Change Root - allow selection of a new root.
* Mode - set the mode to Edit, Move or View.
* Find... - find a node by label.
* Find Again - find the next matching node.

### Select menu

See the above description of the [_Selection Menu Button_](#selection-menu-button).

### Layout menu

See the above description of the [_Layout Menu Button_](#label-menu-button).

### View menu

* Use Dark Theme - turn dark mode on or off.
* Increase Font Size - increase the font size ( urrently not implemented).
* Decrease Font Size decrease the font size ( urrently not implemented).
* Zoom In - zoom in.
* Zoom Out - zoom out.
* Enter Full Screen - enter or exit full screen mode.

### Window menu

* About - show program about screen.
* Check for updates - check whether a new release is available.
* List of all currently open windows.

## Tips and Best Practices

* Start in Edit Mode to build the basic structure of your network.
* Use Transform Mode for refining positions and adjusting edges.
* Label nodes early for easier reference during analysis.
* Regularly export versions of your work to keep backups of your progress.

## Support and Feedback

If you encounter any issues or have suggestions for PhyloSketch, please reach out via the GitHub repository or contact
us through the app’s support page.

### Known issues

This is work in progress and there are a number of known issues:

* The program allows you to create multiple trees and/or networks on the same canvas. At present,
  the status bar reports as if all nodes and edges are part of the same graph.
* iOS 18 allows apps be open in a non-full screen fashion. The PhyloSketch app is not aware of this and
  opening in non-full screen and then resizing might not work correctly. Workaround - to force the app to resize,
  briefly rotate the device to change its orientation and then rotate back.
* PhyloSketch currently does not attend to optimize the layout of an imported rooted phylogenetic network. We are
  working
  on a new algorithm to address this.
* Copy and paste between different PhyloSketch documents is currently based only on the Newick format and thus
  does not preserve formating.
* Paste only pastes in a single tree or network, even if the text or document contains more than one.
* The mobile app is currently only available via Apple Testflight.