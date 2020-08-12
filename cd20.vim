" CD20 syntax file

if exists("b:current_syntax")
	finish
end

syn keyword syntaxElementKeyword constants types is arrays
syn keyword syntaxElementKeyword array of func void const
syn keyword syntaxElementKeyword for repeat until input print println
syn keyword syntaxElementKeyword return not and or xor

" Keywords
syntax keyword Conditional if else
syntax keyword Keyword CD20 main begin end

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
hi def link cdTodo Todo
hi def link cdComment Comment

" Strings
syntax region cdString start=+"+ skip=+\\"+ end=+"+
highlight def link cdString String

let b:current_syntax = "cd20"

