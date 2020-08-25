# CD20 Comments

- Cannot return/declare strings, structs, or arrays? A function can return either int, real, or
  bool, but no strings 
- You can only assign to booleans??? Nevermind: <bool> is incredible misleading
- You cannot put `not someBoolean` in an if statement
- Arrays have to contain structs
- The array declaration involves one and only one expression?
- You cannot have zero assignment statements inside a repeat.
- Exclamation (!) is included in grammar specification but not in provided token
  list.
- Provided token list contains CD20 under keywords, but the print version reads
  CD19.
- You cannot use brackets to define order of operations in arithmetic
- Sometimes you use semicolons, sometimes you don't. It's quite confusing.
    I keep slipping into a ruby mindset because it's so ruby like, but the
    semicolons totally ruin that.
- No break or continue statements. So the only way to leave a loop is to either
    return, or have deeply nested if statements. Yuck.
