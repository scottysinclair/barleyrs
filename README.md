# BarleyRS

Provides access to a JDBC supported database over a Restful interface. Uses [BarleyDB](https://github.com/scottysinclair/barleydb).

BarleyRS intended for enterprise use as it is designed for pluginable:
* Audit control of all modifying database operations.
* Access-control of all modifying operations, access-control of querying operations has yet to be implemented.

It is fully possible to create a ReactJS or IOS client with full back-end CRUD operations implemented in BarleyRS server.

## CRUD Services
* GET `/barleyrs/entities/{namespace}/{entityType}/{id}` - Get the data for a entity.
* GET `/barleyrs/entities/{namespace}/{entityType}/` - Get a listing of entity data.
  * Use query parameter proj to control what columns are fetched.
* GET `/barleyrs/tables/{namespace}/{entityType}/` - Get a listing of entity data for displaying in a listing table (names of linked FK entities are displayed)
* POST `/barleyrs/entities/{namespace}/{entityType}` - Saves the given entity to the database.
* DELETE `/barleyrs/entities/{namespace}/{entityType}` - Deletes the given entity from the database.

## Schema Services
* GET `/barleyrs/entitytypes/` - Get the list of all entity typs.
* GET `/barleyrs/entitytypes/{namespace}/{entityType}` - Get the JSON Schema of a entity type.

BarleyRS requires the following modifications in `applicationContext.xml` to work:
* The `datasource` bean in the applicationContext.xml
* The path to the BarleyDB schema configuration XML file.
* The top level namespace for the configuration.


