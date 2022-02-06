# Project Structure
The project is separated into the following packages:

### activity
activity contains MainActivity (the application's sole activity), MainActivity's
super-class NavViewActivity, view models used by the activity, as well as extension functions of
the MainActivityBinding UI class.

MainActivity's UI consists of an instance of ListActionBar (a custom toolbar
implementation), and a bottom navigation drawer. When collapsed, the navigation
drawer contains a BottomAppBar (another custom toolbar implementation), which
itself contains a BottomNavigationView, a checkout button, and an add button.
When expanded, the navigation drawer displays an app settings button and an
ItemGroupSelector (a custom view to manipulate the items shown in a shopping
list or inventory. This activity UI is shared between all fragments.

### model
model contains all entities that would be considered model level in the MVVM
architecture. At the moment it contains navigation state holders for view models
that require the application's navigation state, as well as the sub-package
database. database contains the application's database, Room data access objects,
and POJO representations of the items stored in the database tables.

### fragment
fragment contains all of the fragments used in the app. The two main content
fragments' inheritance chain is:
<pre>
ListViewFragment -> ShoppingListFragment
                 -> InventoryFragment
</pre>

### recyclerview
recyclerview contains all of the classes used in the inheritance chain of the
recycler views used in the two main fragments:
<pre>
ItemListView -> ExpandableItemListView -> ShoppingListView
                                       -> InventoryView
</pre>
and other associated classes (e.g. item views and item animators). The recycler
views' item views' inheritance chain is:
<pre>
ListItemView -> ExpandableItemView -> ShoppingListItemView
                                   -> InventoryItemView
</pre>

The recyclerview package also contains the recycler views used in the
ItemGroupSelector and ItemGroupPicker, along with their parent ItemGroupListView.

### view
view contains all other custom views that don't fit into recyclerview. The main
activity's UI elements, such as the ListActionBar and BottomAppBar, are located
here.

### utils
miscellaneous utility classes, objects, and functions.
