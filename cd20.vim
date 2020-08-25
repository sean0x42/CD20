" CD20 syntax file

if exists("b:current_syntax")
	finish
end


" Keywords
syntax keyword Conditional if else
syntax keyword Keyword CD20 main begin end
syntax keyword Keyword constants types is arrays
syntax keyword Keyword array of func void const
syntax keyword Keyword for repeat until input print println
syntax keyword Keyword return not and or xor

" Identifiers
syntax match cdIdent "\w+" contained
syntax match cdProgram "CD20 \w+" contains=cdIdent
highlight def link cdIdent Identifier

" Bools
syntax keyword Boolean true false

" Types
syntax keyword Type int real bool

" Comments and TODOs
syntax keyword cdTodo contained TODO FIXME NOTE
syntax match cdComment "/--.*$" contains=cdTodo
syntax region cdComment start="/\*\*" end="\*\*/"
hi def link cdTodo Todo
hi def link cdComment Comment

" Strings
syntax region cdString start=+"+ skip=+\\"+ end=+"+
highlight def link cdString String

let b:current_syntax = "cd20"

