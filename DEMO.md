# Demo

This file shows some examples of how to use the World Dimension Nexus mod.

## Creating a New Dimension

To create a new dimension, you can use the `/wdn create` command. This command will prompt you to
enter the name of the new dimension and will create a new folder in the server's `worlds` directory.

Example:

```
/wdn dimension create my_new_dimension
```

### Teleporting Between Dimensions

To teleport between dimensions, you can use the `/tp` command. This command allows you to
specify the target dimension and will teleport you to the spawn point of that dimension.

Example:

```
/execute in world_dimension_nexus:my_new_dimension run tp @p ~ ~ ~
```

## List Dimensions

To list all available dimensions, you can use the `/wdn dimension list` command.
This will display a list of all dimensions currently managed by the World Dimension Nexus mod.

## Portal Blocks

To create a portal block that allows players to teleport between dimensions, you can use the
special portal block provided by the World Dimension Nexus mod.

Just place colored wool blocks in a 5x4 rectangle and place diamond blocks in the corners.
This will create a portal that players can use to teleport between dimensions.
