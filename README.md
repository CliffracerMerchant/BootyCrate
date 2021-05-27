# BootyCrate

BootyCrate is an open source shopping list and simple inventory management app built using Kotlin.
Tech-stack components include:
- Android Architecture Components (Room, ViewModel, LiveData)
- MVVM paradigm (though without a repository layer at the moment)
- Kotlin coroutines, including Flows.
- Espresso for end to end testing

## Features:
- An inventory to which the user can add items that are frequently bought.
- A auto-add to shopping list trigger amount for inventory items.
- A shopping list with items that were either added automatically from inventory
  items that fell below their trigger amount, or were added manually by the user.
- Shopping list item checking and checkout functionality. Checking out will remove
  all checked items from the shopping list, and for items that are linked to an
  inventory item will automatically update the inventory amount.
- Text exporting of either entire lists or selected items.
- Color labels to organize items
- Searching by name, and sorting by color, amount, and name.
- A one time or repeating reminder notification to update your shopping list and / or inventory.

## Planned features:
- Multiple shopping lists and inventories with customizable names
- Customizable (on a per shopping list / inventory basis) labels for each color 
- Auto adding all items from a recipe to the shopping list
- Cloud data backup
- Barcode scanner

## Project Structure
The project is separated into the following packages:

### activity
activity contains all of the classes in the inheritance chain of the single activity used in the app.
These classes are:
MultiFragmentActivity -> MainActivity -> GradientStyledMainActivity

GradientStyledMainActivity's UI consists of an instance of RecyclerViewActionBar (a custom toolbar
implementation), an instance of BottomAppBar (another custom toolbar implementation), a
BottomNavigationViewEx (acts as a BottomNavigationView but with better sub-view access), a checkout
button, and an add button. This activity ui is shared between all fragments.

### fragment
fragment contains all of the fragments used in the app. The two main content fragments' inheritance
chain is:
RecyclerViewFragment -> ShoppingListFragment
					 -> InventoryFragment

### recyclerview
recyclerview contains all of the classes used in the inheritance chain of the recycler views used in
the two main fragments:
BootyCrateRecyclerView -> ExpandableSelectableRecyclerView -> ShoppingListRecyclerView
														   -> InventoryRecyclerView
and other associated classes (e.g. item views and item animators). The recycler views' item views'
inheritance chain is:
BootyCrateItemView -> ExpandableSelectableItemView -> ShoppingListItemView
												   -> InventoryItemView

### database
database contains the applications database, Room data access object, and view models.

### utils
miscellaneous utility classes, objects, and functions.

### view
view contains all other custom views that don't fit into another package. The main activity's ui
elements, such as the RecyclerViewActionBar and BottomAppBar, are located here.

# License
BootyCrate is licensed under the terms of the Apache License, 2.0.