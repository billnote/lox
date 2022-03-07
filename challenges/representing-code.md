1. Produce a grammar without any notational sugar
   * expr -> expr ("(" (expr ("," expr)* )? ")" | "." IDENTIFIER )+
           | IDENTIFIER
           | NUMBER
   
   - ex -> "(" (expr ("," expr)* )? ")" 
         | "." IDENTIFIER
   
   - item -> "," array 
   - array -> expr
   - array -> expr item
   - group -> "(" array ")"
   - group -> "(" ")"
   - group -> "." IDENTIFIER

   - expr -> expr 
   - expr -> IDENTIFIER
   - expr -> NUMBER
   - expr -> expr group

answer:
expr → expr calls
expr → IDENTIFIER
expr → NUMBER

calls → calls call
calls → call

call → "(" ")"
call → "(" arguments ")"
call → "." IDENTIFIER

arguments → expr
arguments → arguments "," expr