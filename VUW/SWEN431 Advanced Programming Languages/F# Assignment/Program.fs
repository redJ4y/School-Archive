open System
open System.IO
open System.Text.RegularExpressions

type UnaryOp = 
    | Not         // !
    | Complement  // ~
    | Eval        // EVAL
type BinaryOp =
    | Add         // +
    | Subtract    // -
    | Multiply    // *
    | Divide      // /
    | Pow         // **
    | Modulo      // %
    | Equals      // ==
    | NotEquals   // !=
    | Greater     // >
    | Lesser      // <
    | GreaterEq   // >=
    | LesserEq    // <=
    | Comparison  // <=>
    | And         // &
    | Or          // |
    | Xor         // ^
    | LeftShift   // <<
    | RightShift  // >>
type StackOp =
    | Drop        // DROP
    | Dup         // DUP
    | Swap        // SWAP
    | Rot         // ROT
    | Roll        // ROLL
    | RollD       // ROLLD
type ConditionalOp =
    | IfElse      // IFELSE

type LiteralType =
    | Int of int
    | Float of float
    | Bool of bool
    | String of string
    | Vector of int array

type Operation =
    | Unary of UnaryOp
    | Binary of BinaryOp
    | Stack of StackOp
    | Conditional of ConditionalOp
    | Literal of LiteralType
    | Lambda of string
    | Quoted of Operation

(* Parse a WackoStacko program string into an Operation list to be interpreted *)
let parse (program: string) : Operation list =
    (* Split a program string into individual operation strings,
     * being mindful of vectors, lambdas, and strings *)
    let splitOps (program: string) : string list =
        let rec opify (chars: char list) (currentOp: string) (inVec: bool) (inLam: bool) (inStr: bool) : string list =
            match chars with
            | [] -> if currentOp = "" then [] else [currentOp]
            | current :: remaining ->
                match current with
                | '"' -> opify remaining (currentOp + string current) inVec inLam (not inStr)
                | '[' | ']' -> opify remaining (currentOp + string current) (not inVec) inLam inStr
                | '{' | '}' -> opify remaining (currentOp + string current) inVec (not inLam) inStr
                | ' ' when not inVec && not inLam && not inStr ->
                    match currentOp with
                    | "" -> opify remaining "" false false false
                    | _  -> currentOp :: opify remaining "" false false false
                | _ -> opify remaining (currentOp + string current) inVec inLam inStr

        opify (program.ToCharArray() |> List.ofArray) "" false false false

    (* Convert a string into an Operation *)
    let rec parseOp (input: string) : Operation =
        match input with
        | "!" -> Unary Not
        | "~" -> Unary Complement
        | "EVAL" -> Unary Eval
        | "+" -> Binary Add
        | "-" -> Binary Subtract
        | "*" -> Binary Multiply
        | "/" -> Binary Divide
        | "**" -> Binary Pow
        | "%" -> Binary Modulo
        | "==" -> Binary Equals
        | "!=" -> Binary NotEquals
        | ">" -> Binary Greater
        | "<" -> Binary Lesser
        | ">=" -> Binary GreaterEq
        | "<=" -> Binary LesserEq
        | "<=>" -> Binary Comparison
        | "&" -> Binary And
        | "|" -> Binary Or
        | "^" -> Binary Xor
        | "<<" -> Binary LeftShift
        | ">>" -> Binary RightShift
        | "DROP" -> Stack Drop
        | "DUP" -> Stack Dup
        | "SWAP" -> Stack Swap
        | "ROT" -> Stack Rot
        | "ROLL" -> Stack Roll
        | "ROLLD" -> Stack RollD
        | "IFELSE" -> Conditional IfElse
        | s when s |> Int32.TryParse |> fst -> Literal (Int (Int32.Parse s))
        | s when s |> Double.TryParse |> fst -> Literal (Float (Double.Parse s))
        | s when s = "true" || s = "false" -> Literal (Bool (Boolean.Parse s))
        | s when s.StartsWith("\"") && s.EndsWith("\"") -> Literal (String (s.Substring(1, s.Length - 2)))
        | s when s.StartsWith("[") && s.EndsWith("]") -> Literal (Vector (s.Substring(1, s.Length - 2).Split(',') |> Array.map Int32.Parse))
        | s when s.StartsWith("{") && s.EndsWith("}") -> Lambda (s.Substring(1, s.Length - 2))
        | s when s.StartsWith("'") -> Quoted (parseOp (s.Substring(1)))
        | _ -> failwithf "Unknown operation: \"%s\"" input

    splitOps program
    |> List.map parseOp 

(* Convert an Operation back into a string *)
let rec stringifyOp (op: Operation) : string =
    match op with
    | Unary Not -> "!"
    | Unary Complement -> "~"
    | Unary Eval -> "EVAL"
    | Binary Add -> "+"
    | Binary Subtract -> "-"
    | Binary Multiply -> "*"
    | Binary Divide -> "/"
    | Binary Pow -> "**"
    | Binary Modulo -> "%"
    | Binary Equals -> "=="
    | Binary NotEquals -> "!="
    | Binary Greater -> ">"
    | Binary Lesser -> "<"
    | Binary GreaterEq -> ">="
    | Binary LesserEq -> "<="
    | Binary Comparison -> "<=>"
    | Binary And -> "&"
    | Binary Or -> "|"
    | Binary Xor -> "^"
    | Binary LeftShift -> "<<"
    | Binary RightShift -> ">>"
    | Stack Drop -> "DROP"
    | Stack Dup -> "DUP"
    | Stack Swap -> "SWAP"
    | Stack Rot -> "ROT"
    | Stack Roll -> "ROLL"
    | Stack RollD -> "ROLLD"
    | Conditional IfElse -> "IFELSE"
    | Literal (Int i) -> string i
    | Literal (Float f) -> sprintf "%g" f
    | Literal (Bool b) -> (string b).ToLower()
    | Literal (String s) -> "\"" + s + "\""
    | Literal (Vector arr) -> "[" + (String.Join(", ", arr)) + "]"
    | Lambda s -> "{" + s + "}"
    | Quoted op -> "'" + stringifyOp op

(* Recursively perform the given Operations on the given stack *)
let rec interpret (stack: Operation list) (ops: Operation list) : Operation list =
    (* Perform the given UnaryOp on the given stack *)
    let unaryOperate (op: UnaryOp) (stack: Operation list) : Operation list =
        match stack with
        | first :: remainingStack ->
            match op, first with
            | Not, Literal (Bool b) -> Literal (Bool (not b)) :: remainingStack
            | Complement, Literal (Int i) -> Literal (Int (~~~i)) :: remainingStack
            | Eval, _ -> interpret remainingStack [first]
            | _ -> failwith "Invalid operand for UnaryOp"
        | _ -> failwith "Insufficient stack for UnaryOp"

    (* Perform the given BinaryOp on the given stack *)
    let binaryOperate (op: BinaryOp) (stack: Operation list) : Operation list =
        let binaryOperateInt (op: BinaryOp) (i1: int) (i2: int) : LiteralType =
            match op with
            | Add -> Int (i1 + i2)
            | Subtract -> Int (i1 - i2)
            | Multiply -> Int (i1 * i2)
            | Divide -> Int (i1 / i2)
            | Pow -> Int (int (float i1 ** float i2))
            | Modulo -> Int (i1 % i2)
            | Equals -> Bool (i1 = i2)
            | NotEquals -> Bool (i1 <> i2)
            | Greater -> Bool (i1 > i2)
            | Lesser -> Bool (i1 < i2)
            | GreaterEq -> Bool (i1 >= i2)
            | LesserEq -> Bool (i1 <= i2)
            | Comparison -> Int (if i1 < i2 then -1 elif i1 = i2 then 0 else 1)
            | And -> Int (i1 &&& i2)
            | Or -> Int (i1 ||| i2)
            | Xor  -> Int (i1 ^^^ i2)
            | LeftShift -> Int (i1 <<< i2)
            | RightShift -> Int (i1 >>> i2)
        let binaryOperateFloat (op: BinaryOp) (f1: float) (f2: float) : LiteralType =
            match op with
            | Add -> Float (f1 + f2)
            | Subtract -> Float (f1 - f2)
            | Multiply -> Float (f1 * f2)
            | Divide -> Float (f1 / f2)
            | Pow -> Float (f1 ** f2)
            | Modulo -> Float (f1 % f2)
            | Equals -> Bool (f1 = f2)
            | NotEquals -> Bool (f1 <> f2)
            | Greater -> Bool (f1 > f2)
            | Lesser -> Bool (f1 < f2)
            | GreaterEq -> Bool (f1 >= f2)
            | LesserEq -> Bool (f1 <= f2)
            | Comparison -> Float (if f1 < f2 then -1 elif f1 = f2 then 0 else 1)
            | _ -> failwith "Invalid BinaryOp given operand type Float"
        let binaryOperateBool (op: BinaryOp) (b1: bool) (b2: bool) : LiteralType =
            match op with
            | And -> Bool (b1 && b2)
            | Or -> Bool (b1 || b2)
            | Xor -> Bool ((b1 && not b2) || (not b1 && b2))
            | _ -> failwith "Invalid BinaryOp given operand type Bool"
        let binaryOperateString (op: BinaryOp) (s1: string) (s2: string) : LiteralType =
            match op with
            | Add -> String (s1 + s2)
            | Multiply -> String (String.replicate (int s2) s1)
            | Equals -> Bool (s1 = s2)
            | NotEquals -> Bool (s1 <> s2)
            | Greater -> Bool (s1 > s2)
            | Lesser -> Bool (s1 < s2)
            | GreaterEq -> Bool (s1 >= s2)
            | LesserEq -> Bool (s1 <= s2)
            | Comparison -> Int (if s1 < s2 then -1 elif s1 = s2 then 0 else 1)
            | _ -> failwith "Invalid BinaryOp given operand type String"
        let binaryOperateVector (op: BinaryOp) (v1: int array) (v2: int array) : LiteralType =
            match op with
            | Add -> Vector (Array.map2 (+) v1 v2)
            | _ -> failwith "Invalid BinaryOp given operand type Vector"

        match stack with
        | Literal first :: Literal second :: remainingStack ->
            let result = 
                match second, first with
                | Int i1, Int i2 -> binaryOperateInt op i1 i2
                | Float f1, Float f2 -> binaryOperateFloat op f1 f2
                | Int i, Float f -> binaryOperateFloat op (float i) f
                | Float f, Int i -> binaryOperateFloat op f (float i)
                | Bool b1, Bool b2 -> binaryOperateBool op b1 b2
                | String s1, String s2 -> binaryOperateString op s1 s2
                | String s, Int i -> binaryOperateString op s (string i)
                | Vector v1, Vector v2 -> binaryOperateVector op v1 v2
                | _ -> failwith "Invalid operand(s) for BinaryOp"
            Literal result :: remainingStack
        | _ -> failwith "Insufficient stack for BinaryOp"

    (* Perform the given StackOp on the given stack *)
    let stackOperate (op: StackOp) (stack: Operation list) : Operation list =
        match op, stack with
        | Drop, _ :: remainingStack -> remainingStack
        | Dup, first :: remainingStack -> first :: first :: remainingStack
        | Swap, first :: second :: remainingStack -> second :: first :: remainingStack
        | Rot, third :: second :: first :: remainingStack -> first :: third :: second :: remainingStack
        | Roll, (Literal (Int n)) :: remainingStack ->
            match List.splitAt (n - 1) remainingStack with
            | (before, toRoll :: after) -> toRoll :: before @ after
            | _ -> failwith "Insufficient stack for StackOp Roll"
        | RollD, (Literal (Int n)) :: toRoll :: remainingStack ->
            let before, after = List.splitAt (n - 1) remainingStack
            before @ (toRoll :: after)
        | _ -> failwith "Insufficient stack for StackOp"

    (* Perform the given ConditionalOp on the given stack *)
    let conditionalOperate (op: ConditionalOp) (stack: Operation list) : Operation list =
        match stack with
        | Literal (Bool condition) :: falseVal :: trueVal :: remainingStack ->
            if condition then
                trueVal :: remainingStack
            else
                falseVal :: remainingStack
        | _ -> failwith "Insufficient stack for ConditionalOp"

    (* Parse, interpret, and perform the given Lambda string on the given stack *)
    let evalLambda (lam: string) (stack: Operation list) : Operation list =
        let splitIndex = lam.IndexOf('|')
        let body = lam.Substring(splitIndex + 1).Trim()
        let numParams = int (lam.Substring(0, splitIndex).Trim())
        let argsBackwards, remainingStack = List.splitAt numParams stack
        let args = List.rev argsBackwards

        let substituteArgs (m: Match) =
            let index = int m.Groups.[1].Value
            stringifyOp args.[index]

        let bodyWithArgs = Regex.Replace(body, @"x(\d+)", substituteArgs)
        let finalBody = bodyWithArgs.Replace("SELF", sprintf "'{%s}" lam)

        (parse finalBody |> interpret []) @ remainingStack

    (* Determine the stack size requirement of the given Operation *)
    let lenReqOf (op: Operation) =
        match op with
        | Unary _ -> 1
        | Binary _ -> 2
        | Stack op ->
            match op with
            | Drop -> 1
            | Dup -> 1
            | Swap -> 2
            | Rot -> 3
            | Roll -> 2
            | RollD -> 2
        | Conditional _ -> 3
        | Literal _ -> 0
        | Lambda _ -> 0
        | Quoted _ -> 0

    match ops with
    | [] -> stack
    | op :: remainingOps ->
        if List.length stack < lenReqOf op then
            interpret (op :: stack) remainingOps
        else
            let newStack = 
                match op with
                | Literal _ -> op :: stack
                | Quoted innerOp -> innerOp :: stack
                | Unary op -> unaryOperate op stack
                | Binary op -> binaryOperate op stack
                | Stack op -> stackOperate op stack
                | Conditional op -> conditionalOperate op stack
                | Lambda lam -> evalLambda lam stack
            interpret newStack remainingOps

(* This function is expected to take in the complete source text
 * and to return a list of strings suitable for writing to the
 * output file. *)
let processSource (sourceText: string) : string list =
    sourceText.Replace("\r", "").Replace("\n", " ")
    |> parse
    |> interpret []
    |> List.rev
    |> List.map stringifyOp

(***************************************************************)
(* The rest of the code sets up the file reading and writing. *)

(* Filename given on command line: dotnet run input-xyz.txt *)
let inputFilename = Environment.GetCommandLineArgs()[1]

(* Read full contents of that file into a string *)
let sourceText = File.ReadAllText(inputFilename).Trim()

(* This line calls the function from the top of the file
 * that does all of the main work. You can change how
 * this works if you like. *)
let resultLines = processSource sourceText

(* Calculate output filename *)
let outputFilename = inputFilename.Replace("input-", "output-")

(* Write to the output file, one line for each of resultLines *)
File.WriteAllLines(outputFilename, resultLines)
