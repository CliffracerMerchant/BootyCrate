# Project Structure
The project is separated into the following packages:

### activity
activity contains all of the classes in the inheritance chain of the single activity
used in the app, as well as extension functions of the MainActivityBinding UI class. 
These classes are:
<pre>
BottomNavViewActivity -> MainActivity
</pre>

MainActivity's UI consists of an instance of ListActionBar (a custom toolbar
implementation), an instance of BottomAppBar (another custom toolbar implementation),
a BottomNavigationView, a checkout button, and an add button. This activity UI is
shared between all fragments.

### database
database contains the application's database, Room data access objects, and
POJO representations of the items stored in the database tables.

### fragment
fragment contains all of the fragments used in the app. The two main content fragments'
inheritance chain is:
<pre>
ListViewFragment -> ShoppingListFragment
                 -> InventoryFragment
</pre>

### recyclerview
recyclerview contains all of the classes used in the inheritance chain of the recycler
views used in the two main fragments:
<pre>
ItemListView -> ExpandableItemListView -> ShoppingListView
                                       -> InventoryView
</pre>
and other associated classes (e.g. item views and item animators). The recycler views'
item views' inheritance chain is:
<pre>
ListItemView -> ExpandableItemView -> ShoppingListItemView
                                   -> InventoryItemView
</pre>

### view
view contains all other custom views that don't fit into another package. The main
activity's UI elements, such as the ListActionBar and BottomAppBar, are located here.

### viewmodel
viewmodel contains all of the application's view models.

### utils
miscellaneous utility classes, objects, and functions.
