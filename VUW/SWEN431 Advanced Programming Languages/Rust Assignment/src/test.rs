#[cfg(test)]
mod arith_tests {
    use crate::expression::Expression;
    use crate::parser::Parser;

    #[test]
    fn parse_var() {
        let mut prog = Parser::new(&"x");
        let result = prog.parse();
        assert!(result.is_ok());
        let e = result.unwrap();
        assert_eq!("x", format!("{}", e));
    }

    #[test]
    fn parse_int() {
        let mut prog = Parser::new(&"123");
        let result = prog.parse();
        assert!(result.is_ok());
        let e = result.unwrap();
        assert_eq!("123", format!("{}", e));
    }

    #[test]
    fn parse_bool() {
        let mut prog = Parser::new(&"T");
        let result = prog.parse();
        assert!(result.is_ok());
        let e = result.unwrap();
        assert_eq!("T", format!("{}", e));
    }

    #[test]
    fn parse_plus() {
        let mut prog = Parser::new(&"+(1, 1)");
        let result = prog.parse();
        assert!(result.is_ok());
        let e = result.unwrap();
        assert_eq!("1 + 1", format!("{}", e));
    }

    #[test]
    fn parse_minus() {
        let mut prog = Parser::new(&"-(1, 1)");
        let result = prog.parse();
        assert!(result.is_ok());
        let e = result.unwrap();
        assert_eq!("1 - 1", format!("{}", e));
    }

    #[test]
    fn parse_mult() {
        let mut prog = Parser::new(&" *(1, 1)");
        let result = prog.parse();
        assert!(result.is_ok());
        let e = result.unwrap();
        assert_eq!("1 * 1", format!("{}", e));
    }

    #[test]
    fn parse_div() {
        let mut prog = Parser::new(&"/ (1, 1)");
        let result = prog.parse();
        assert!(result.is_ok());
        let e = result.unwrap();
        assert_eq!("1 / 1", format!("{}", e));
    }

    #[test]
    fn parse_lt() {
        let mut prog = Parser::new(&"< (1, 1)");
        let result = prog.parse();
        assert!(result.is_ok());
        let e = result.unwrap();
        assert_eq!("1 < 1", format!("{}", e));
    }

    #[test]
    fn parse_not() {
        let mut prog = Parser::new(&"! T");
        let result = prog.parse();
        assert!(result.is_ok());
        let e = result.unwrap();
        assert_eq!("!T", format!("{}", e));
    }

    #[test]
    fn parse_eq() {
        let mut prog = Parser::new(&"=( 1, 1)");
        let result = prog.parse();
        assert!(result.is_ok());
        let e = result.unwrap();
        assert_eq!("1 = 1", format!("{}", e));
    }

    #[test]
    fn parse_func() {
        let mut prog = Parser::new(&"func x =>  T");
        let result = prog.parse();
        assert!(result.is_ok());
        let e = result.unwrap();
        assert_eq!("func x => T", format!("{}", e));
    }

    #[test]
    fn parse_app() {
        let mut prog = Parser::new(&"apply (  func x =>  x , 1)");
        let result = prog.parse();
        assert!(result.is_ok());
        let e = result.unwrap();
        assert_eq!("func x => x (1)", format!("{}", e));
    }

    #[test]
    fn parse_if() {
        let mut prog = Parser::new(&"if < (1, 5  ) then   8 else 9");
        let result = prog.parse();
        assert!(result.is_ok());
        let e = result.unwrap();
        assert_eq!("if 1 < 5 then 8 else 9", format!("{}", e));
    }

    #[test]
    fn eval_test_1() {
        let mut prog = Parser::new(&"if <(1, 5) then 8 else 9");
        let ast = prog.parse();
        assert!(ast.is_ok());
        let e = ast.unwrap().eval();
        assert!(e.is_ok());
        let result = e.unwrap();
        assert_eq!(Expression::Int(8), result);
    }

    #[test]
    fn eval_test_2() {
        let mut prog = Parser::new(&"if <(1, 5) then T else F");
        let ast = prog.parse();
        assert!(ast.is_ok());
        let e = ast.unwrap().eval();
        assert!(e.is_ok());
        let result = e.unwrap();
        assert_eq!(Expression::True, result);
    }

    #[test]
    fn eval_test_3() {
        let mut prog = Parser::new(&"*(+(1, 4), 5)");
        let ast = prog.parse();
        assert!(ast.is_ok());
        let e = ast.unwrap().eval();
        assert!(e.is_ok());
        let result = e.unwrap();
        assert_eq!(Expression::Int(25), result);
    }

    #[test]
    fn eval_test_4() {
        let mut prog = Parser::new(&"apply(func x => -(x, 5), +(1, 9))");
        let ast = prog.parse();
        assert!(ast.is_ok());
        let e = ast.unwrap().eval();
        assert!(e.is_ok());
        let result = e.unwrap();
        assert_eq!(Expression::Int(5), result);
    }

    #[test]
    fn eval_test_5() {
        let mut prog = Parser::new(&"apply(apply(func x => func x => x, 1), 2)");
        let ast = prog.parse();
        assert!(ast.is_ok());
        let e = ast.unwrap().eval();
        assert!(e.is_ok());
        let result = e.unwrap();
        assert_eq!(Expression::Int(2), result);
    }

    #[test]
    fn eval_test_6() {
        let mut prog = Parser::new(&"apply(apply(func x => func y => +(*(x, x), *(y, y)), 3), 5)");
        let ast = prog.parse();
        assert!(ast.is_ok());
        let e = ast.unwrap().eval();
        assert!(e.is_ok());
        let result = e.unwrap();
        assert_eq!(Expression::Int(34), result);
    }

    #[test]
    fn eval_test_7() {
        let mut prog = Parser::new(&"if !F then x else y");
        let ast = prog.parse();
        assert!(ast.is_ok());
        let e = ast.unwrap().eval();
        assert!(e.is_ok());
        let result = e.unwrap();
        assert_eq!(Expression::Variable('x'), result);
    }

    #[test]
    fn eval_test_8() {
        let mut prog = Parser::new(&"func x => x");
        let ast = prog.parse();
        assert!(ast.is_ok());
        let e = ast.unwrap().eval();
        assert!(e.is_ok());
        let result = e.unwrap();
        assert_eq!(
            Expression::Func(
                Box::new(Expression::Variable('x')),
                Box::new(Expression::Variable('x'))
            ),
            result
        );
    }

    #[test]
    fn apply_func_with_func() {
        let mut prog = Parser::new(&"apply(func x => x, func y => 1)");
        let ast = prog.parse();
        assert!(ast.is_ok());
        let e = ast.unwrap().eval();
        assert!(e.is_ok());
        let result = e.unwrap();
        assert_eq!(
            Expression::Func(
                Box::new(Expression::Variable('y')),
                Box::new(Expression::Int(1))
            ),
            result
        );
    }
}
