# BarleyRS

Provides Restful services for full CRUD access to schemas and data in [BarlyDB](https://github.com/scottysinclair/barleydb). 

* GET `/barleyrs/entities/{namespace}/{entityType}/{id}` - Get the data for a entity.
* GET `/barleyrs/entities/{namespace}/{entityType}/` - Get a listing of entity data.
  * Use query parameter proj to control what columns are fetched.
* GET `/barleyrs/tables/{namespace}/{entityType}/` - Get a listing of entity data for displaying in a listing table (names of linked FK entities are displayed)
* POST `/barleyrs/entities/{namespace}/{entityType}` - Saves the given entity to the database.
* DELETE `/barleyrs/entities/{namespace}/{entityType}` - Deletes the given entity from the database.


