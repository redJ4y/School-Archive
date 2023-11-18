# Student Manager Application

A simple interface to view student records, allowing users to fetch individual records by ID, fetch all records, or export all records to a CSV file.

## How to run the application

After running `mvn package`, you can execute the resulting "uber-jar" as follows:

```bash
java -jar target/studentfinder.jar <arguments>
```

**Available arguments**:

- `-fetchone <id>`: Fetch the student record with the specified ID.
- `-fetchall`: Fetch all student records.
- `-export -f <file>`: Export all student records to a file in CSV format.

## Testing the application

Tests can be run using the following Maven command:

```bash
mvn test
```

## Memory management

The `StudentManager` caches all records retrieved from the database in hashmaps called `studentCache` and `degreeCache`. Holding references in these `Student` and `Degree` objects interferes with the Java garbage collector, which will not deallocate the memory while it is referenced. To mitigate potential memory leaks, we could:

1. **Use weak references**: Using weak references would allow the garbage collector to reclaim `Student` and `Degree` objects when they become weakly reachable. This could be achieved using `WeakHashMap` from Java or `CacheBuilder` from the Guava library which can be configured to use weak keys or values.
2. **Limit cache sizes**: Limiting the size of the caches and removing least recently accessed entries when the limit is reached would place an upper bound on memory usage.
3. **Implement time-based eviction**: Setting an expiration time on cached entries and evicting them after the duration elapses would greatly reduce the chances of using up too much memory.

Google's Guava library would be good for all three of these approaches, as it offers a `CacheBuilder` that provides both size-based and time-based eviction strategies in addition to enabling weak references. However, memory usage safety was not required by the assignment brief.
