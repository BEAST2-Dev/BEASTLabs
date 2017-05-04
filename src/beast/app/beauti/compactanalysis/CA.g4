// grammar

grammar CA;
 
// parser rules
  
casentence : ((template | subtemplate | import_ | partition | link | unlink | set) SEMI)*;

// template
template : TEMPLATETOKEN templatename;

templatename : STRING;


// subtemplate
subtemplate : (idpattern EQ)? SUBTOKEN STRING ( OPENP key EQ value (COMMA key EQ value)* CLOSEP )?;

idpattern : STRING;

key : STRING;

value : STRING;

import_ : IMPORTTOKEN alignmentprovider? filename ( OPENP arg (COMMA arg)* CLOSEP )?;
//
filename :  STRING;
//
alignmentprovider : STRING;
//
arg : STRING;
//
partition : PARTITIONTOKEN partitionpattern;
//
partitionpattern : STRING;

link : LINKTOKEN STRING;

unlink : UNLINKTOKEN STRING;

set : SETTOKEN STRING EQ STRING;


// Lexer Rules

// lexer grammar CALexer;

SEMI: ';' ;
COMMA: ',' ;
OPENP: '(' ;
CLOSEP: ')' ;
EQ: '=' ;

TEMPLATETOKEN : 'template';
IMPORTTOKEN : 'import';
PARTITIONTOKEN : 'partition';
LINKTOKEN : 'link';
UNLINKTOKEN : 'unlink';
SETTOKEN : 'set';
SUBTOKEN : 'sub';

STRING :
    [a-zA-Z0-9|#*%/.\-+_&]+  // these chars don't need quotes
    | '"' .*? '"'
    | '\'' .*? '\''
    ;

WHITESPACE : ( '\t' | ' ' | '\r' | '\n'| '\u000C' )+ -> skip ;