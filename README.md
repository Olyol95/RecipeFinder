<html>

<img src="http://s2.postimg.org/dwsg6z5ih/recipefinderlogo.png" alt="Logo" align="middle">

<h1>Ever forgotten the recipe for an item?</h1>

Now, with Recipe Finder, you can find the recipe of an item with as little effort as a single command!

<h2>Current Features:</h2>

<ul>
<li>/recipe &lt;item name&gt; will display the recipes for all items that match the given item name.</li>
<li>/recipe on its own will display the recipes of the item in your hand.</li>
<li>Multiple recipes will be scrolled through one by one.</li>
<li>Clicking an item in a recipe will display the recipes of said item.</li>
<li>Smelting recipes are also supported.</li>
<li>Names are matched in such a way that commands such as /recipe chestplate will display the recipes for all types of chestplate.</li>
<li>The plugin hooks into the Bukkit/Spigot API, and hence is lightweight, fast and scalable.</li>
<li>Any recipe registered on the server through the API should be visible to the plugin.</li>
<li>Threading ensures that multiple users can access the recipe list simultaneously.</li>
<li>Recipes for coloured wool and dyes also supported!</li>
<li>Multiple languages and renaming of items is now fully supported!</li>
<li>Memory efficient option available at the expense of name-familiarity</li>
</ul>

<h2>Commands:</h2>
<ul>
<li>/recipe <item name>	Displays the recipes associated with the item with the given name.</li>
<li>/recipe	Displays the recipes associated with the item in your hand.</li>
<li>/recipereload	Reloads the plugin from config.</li>
</ul>

<h2>Permissions:</h2>

<ul>
<li>recipe.lookup: gives access to /recipe.</li>
<li>recipe.reload: gives access to /recipereload.</li>
</ul>

<h2>Configuration:</h2>

When you first start up the server with the plugin installed, a new folder will be generated in your 'plugins' directory that is called 'RecipeFinder'. Inside this folder you will find two more folders, named 'config' and 'lang' respectively. Inside the 'config' directory you will find a file called 'config.txt'. This file is used to configure the Recipe Finder plugin. Inside the file you will see the following options:

<table style="width:100%">
  <tr>
    <th>Option</th>
    <td>Meaning</td> 
    <td>Default Value</td>
  </tr>
  <tr>
    <td>languages enabled:</td>
    <td><b>This option should only be used for server admins who have critical memory/performance requirements.</b> This will disable the ability to rename items as well as change the language of the plugin. The name of the items in the lookup will default to the given API names, which can sometimes be obscure such as 'lightstone' and 'cloth leggings', however the plugin will not spend time parsing language files or store translations in memory. This option when set to false is more time and memory efficient therefore, at the cost of usability.</td> 
    <td>true</td>
  </tr>
    <tr>
    <td>language file:</td>
    <td>This refers to the language file, located inside the 'RecipeFinder/lang' directory, that you wish to use for translations or renaming of lookup names</td> 
    <td>en_US.lang</td>
  </tr>
</table>

If you wish to change the language of the plugin, or simply rename an item, create a copy of the file called 'en_US.lang' inside the 'RecipeFinder/lang' directory (or paste in your own) and change the names of the items to whatever values you see fit. Note that many of the options contained in the lang file are redundant, you are looking for entries only beginning in tile. or item.. Once you have done this, go into 'RecipeFinder/config/config.txt' and change the language file: option to be the name of your new language file.

If your server is still running whilst you change the language file, you can use the /recipereload command to reload the config and update the plugin to reflect the changes that you just made.

<h1>important</h1>

The default en_US.lang file is maintained by the plugin and kept up to date, this means that if an update for the default language file is found <b>the plugin will replace the en_US.lang file, meaning any changes you made to that file will be removed</b>. It is therefore important that when changing the names of items, or changing the language entirely, that you <b>use a language file name other than en_US.lang</b>!

If you run into any issues with your config file or default language file, simply delete them and run /recipereload and the plugin will generate the default files for you.

<h2>Features to hopefully be implemented in the future:</h2>

<ul>
<li>Potion recipes.</li>
<li>Firework recipes.</li>
</ul>

</html>
