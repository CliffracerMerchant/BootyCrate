# Project Structure
The project is separated into the following packages:

### activity
activity contains all of the classes in the inheritance chain of the single activity
used in the app, as well as extension functions of the MainActivityBinding UI class. 
These classes are:
<pre>
NavViewActivity -> MainActivity
</pre>

MainActivity's UI consists of an instance of ListActionBar (a custom toolbar
implementation), and a bottom navigation drawer. When collapsed, the navigation
drawer contains a BottomAppBar (another custom toolbar implementation), which
itself contains a BottomNavigationView, a checkout button, and an add button.
When expanded, the navigation drawer displays an app settings button and an
ItemGroupSelector (a custom view to manipulate the items shown in a shopping
list or inventory. This activity UI is shared between all fragments.

### database
database contains the application's database, Room data access objects, and
POJO representations of the items stored in the database tables.

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

### viewmodel
viewmodel contains all of the application's view models, along with state
objects that contain non-persistent state that needs to be shared between
view models, but which is not stored in the app's database (e.g. the
current navigation state of the single activity.

### utils
miscellaneous utility classes, objects, and functions.
