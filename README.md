# Commands

Run a file
```bash
clear; javac -d out src/lox/*.java; java -cp out lox.Lox <PATH_TO_FILE>
```

Run in interactive mode

```bash
clear; javac -d out src/lox/*.java; java -cp out lox.Lox
```

Generate AST
```bash
clear; javac -d out src/tool/*.java; java -cp out tool.GenerateAst src/lox
```

# Recommended Examples

`examples/linear_flow_shape.lox` - Demonstrates linear flow and lambda functions.
`examples/constricted_dam_flow.lox` - Simulates flow through a constricted dam.
`examples/provided_system.rlox` - System provided in the assignment.
