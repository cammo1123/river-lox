-   What literal in your language represents a river that gets 10L/s of flow on the first day after 1mm of rainfall?

To represent a river that receives 10L/s of flow on the first day after 1mm of rainfall, you define a river node with the following properties. It would look like this:

```rust
river River1 {
  area: 1sqm,
  flow_days: 1,
  flow_shape: (day, maxDays) => 1
};
```

-   `area: 10sqm` means rainfall is distributed over 10 square meters.
-   `flow_days: 1` indicates that the river responds to rainfall over a period of 1 day.
-   `flow_shape: (day, maxDays) => 1` means that every day 100% of the inflow flows out. Regardless of the number of days or the percentage, the river will only output 100% of the flow.

If you provide 1mm of rainfall, the river will output 10L on the first day.

-   What symbol in your language is used to show two rivers combine?

The symbol used to show two rivers combine in this language is the double greater-than arrow: >>.

```rust
riverA >> riverB;
```

This means the outflow from riverA is added to riverB. And can be interpreted as "riverA flows into riverB".

-   Is the above symbol a "unary", "binary", or "literal"?

The symbol ">>" is a binary operator. It takes two operands: the river on the left side (the source river) and the river on the right side (the destination river). The operation adds the destination river as a downstream receiver of the source river's outflow.

-   What folder is the "working folder" to compile your parser?

`./src`

-   What command(s) will compile your parser?

`javac -d bin lox/*.java; java -cp bin lox.Lox`

-   In your language, how long does it take all the water to work through a river system after 1 day of rain?

The time it takes for all the water to work through a river system after 1 day of rain depends on the `flow_days` and `flow_shape` properties defined for each river in the system. Each river can have its own response time and flow distribution shape, which determines how quickly water moves through the system.

where `flow_days` indicates the number of days over which the river responds to rainfall, and `flow_shape` defines how the flow is distributed over those days as a function of the day and the maximum number of days.

-   Does your language include statements or is it an expression language?

It includes both statements and expressions. You can define river nodes, connect them, assign values, and use function calls as statements, while also using expressions within those statements.

-   Which chapter of the book have you used as the starting point for your solution?

Chapter 13. So the whole jlox compiler.
