use crate::expression::Expression;

pub struct Parser {
    program: String,
}

impl Parser {
    pub fn new(program: &str) -> Self {
        Self {
            // Reverse string for use as a stack with pop()
            program: program.chars().rev().collect::<String>(),
        }
    }

    pub fn parse(&mut self) -> Result<Expression, String> {
        self.pop_whitespace();
        match self.program.pop() {
            Some('T') => Ok(Expression::True),
            Some('F') => Ok(Expression::False),
            Some('!') => Ok(Expression::Not(Box::new(self.parse()?))),
            Some('<') => self.parse_binary_op(Expression::LessThan),
            Some('=') => self.parse_binary_op(Expression::Equals),
            Some('+') => self.parse_binary_op(Expression::Add),
            Some('-') => self.parse_binary_op(Expression::Subtract),
            Some('*') => self.parse_binary_op(Expression::Multiply),
            Some('/') => self.parse_binary_op(Expression::Divide),
            Some('&') => self.parse_binary_op(Expression::And),
            Some('|') => self.parse_binary_op(Expression::Or),
            Some('i') => {
                // Look ahead for 'f'
                if self.program.chars().last() == Some('f') {
                    self.parse_if()
                        .map_err(|e| format!("Failed to parse 'if': {}", e))
                } else {
                    Ok(Expression::Variable('i')) // Allow for Variable named 'i'
                }
            }
            Some('a') => {
                // Look ahead for 'p' (indicating "apply")
                if self.program.chars().last() == Some('p') {
                    self.parse_apply()
                        .map_err(|e| format!("Failed to parse 'apply': {}", e))
                } else {
                    Ok(Expression::Variable('a')) // Allow for Variable named 'a'
                }
            }
            Some('f') => {
                // Look ahead for 'u' (indicating "func")
                if self.program.chars().last() == Some('u') {
                    self.parse_func()
                        .map_err(|e| format!("Failed to parse 'func': {}", e))
                } else {
                    Ok(Expression::Variable('f')) // Allow for Variable named 'f'
                }
            }
            Some(c) if c.is_digit(10) => self.parse_int(c),
            Some(c) if c.is_ascii_lowercase() => Ok(Expression::Variable(c)),
            Some(c) => Err(format!("Unknown token '{}'", c)),
            None => Err("Blank program".to_owned()),
        }
    }

    fn parse_binary_op(
        &mut self,
        constructor: fn(Box<Expression>, Box<Expression>) -> Expression,
    ) -> Result<Expression, String> {
        self.pop_next_token('(')?;
        let e1 = Box::new(self.parse()?);
        self.pop_next_token(',')?;
        let e2 = Box::new(self.parse()?);
        self.pop_next_token(')')?;
        Ok(constructor(e1, e2)) // Use Expression constructor given
    }

    fn parse_if(&mut self) -> Result<Expression, String> {
        self.program.pop(); // Next char is known to be 'f'
        let cond = Box::new(self.parse()?);
        self.pop_next_string("then")?;
        let e1 = Box::new(self.parse()?);
        self.pop_next_string("else")?;
        let e2 = Box::new(self.parse()?);
        Ok(Expression::If(cond, e1, e2))
    }

    fn parse_apply(&mut self) -> Result<Expression, String> {
        self.program.pop(); // Next char is known to be 'p'
        self.pop_string("ply")?;
        self.pop_next_token('(')?;
        let func = Box::new(self.parse()?);
        self.pop_next_token(',')?;
        let e = Box::new(self.parse()?);
        self.pop_next_token(')')?;
        Ok(Expression::Apply(func, e))
    }

    fn parse_func(&mut self) -> Result<Expression, String> {
        self.program.pop(); // Next char is known to be 'u'
        self.pop_string("nc")?;
        self.pop_whitespace();
        if let Some(c) = self.program.pop() {
            // Func Variable variant enforced during parsing
            if c.is_ascii_lowercase() {
                let x = Box::new(Expression::Variable(c));
                self.pop_next_string("=>")?;
                let e = Box::new(self.parse()?);
                Ok(Expression::Func(x, e))
            } else {
                Err(format!("'func' expected Variable but found '{}'", c))
            }
        } else {
            Err("'func' expected Variable but reached end of program".to_owned())
        }
    }

    fn parse_int(&mut self, first_digit: char) -> Result<Expression, String> {
        let mut i = first_digit as u32 - '0' as u32;
        while let Some(next_char) = self.program.chars().last() {
            if next_char.is_digit(10) {
                i = i * 10 + (self.program.pop().unwrap() as u32 - '0' as u32);
            } else {
                break;
            }
        }
        Ok(Expression::Int(i))
    }

    fn pop_next_string(&mut self, expected_string: &str) -> Result<(), String> {
        self.pop_whitespace();
        self.pop_string(expected_string)?;
        Ok(())
    }

    fn pop_string(&mut self, expected_string: &str) -> Result<(), String> {
        for expected_token in expected_string.chars() {
            self.pop_token(expected_token)?;
        }
        Ok(())
    }

    fn pop_next_token(&mut self, expected_token: char) -> Result<(), String> {
        self.pop_whitespace();
        self.pop_token(expected_token)?;
        Ok(())
    }

    fn pop_token(&mut self, expected_token: char) -> Result<(), String> {
        match self.program.pop() {
            Some(c) if c == expected_token => Ok(()),
            Some(c) => Err(format!("Expected '{}' but found '{}'", expected_token, c)),
            None => Err(format!(
                "Expected '{}' but reached end of program",
                expected_token
            )),
        }
    }

    fn pop_whitespace(&mut self) {
        while let Some(c) = self.program.chars().last() {
            if c.is_whitespace() {
                self.program.pop();
            } else {
                break;
            }
        }
    }
}
