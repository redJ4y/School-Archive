require 'matrix'

class WackoStacko
  def initialize
    @stack = []
    @op_codes = {
      unary: %w[! ~ TRANSP EVAL],
      binary: %w[+ - * / ** % == != > < >= <= <=> & | ^ << >> x],
      stack: %w[DROP DUP SWAP ROT ROLL ROLLD],
      conditional: %w[IFELSE]
    }
    @all_ops = @op_codes.values.flatten
    @len_reqs = {
      unary: 1,
      binary: 2,
      stack: { 'DROP' => 1, 'DUP' => 1, 'SWAP' => 2, 'ROT' => 3, 'ROLL' => 2, 'ROLLD' => 2 },
      conditional: 3
    }
  end

  def perform_operation(op)
    type = @op_codes.find { |_k, v| v.include?(op) }&.first
    if type
      len_req = @len_reqs[type].is_a?(Hash) ? @len_reqs[type][op] : @len_reqs[type]
      if @stack.length < len_req
        @stack.push(op)
        return
      end
      case type
      when :unary then unary_operate(op)
      when :binary then binary_operate(op)
      when :stack then stack_operate(op)
      when :conditional then conditional_operate(op)
      end
    elsif op.start_with?("'")
      extracted_op = op[1..-1]
      if @all_ops.include?(extracted_op) || extracted_op.start_with?('{')
        @stack.push(extracted_op)
      else
        parse_literal(extracted_op)
      end
    elsif op.start_with?('{')
      eval_lambda(op)
    else
      parse_literal(op)
    end
  end

  def unary_operate(op)
    case op
    when '!' then @stack.push(!@stack.pop)
    when '~' then @stack.push(~@stack.pop)
    when 'TRANSP' then @stack.push(@stack.pop.transpose)
    when 'EVAL' then perform_operation(@stack.pop)
    end
  end

  def binary_operate(op)
    second = @stack.pop
    first = @stack.pop
    if first.is_a?(Vector) && second.is_a?(Vector)
      case op
      when '+' then @stack.push(first + second)
      when '*' then @stack.push(first.dot(second))
      when 'x' then @stack.push(first.cross_product(second))
      end
    else
      @stack.push(toggle_quotes(toggle_quotes(first).send(op, toggle_quotes(second))))
    end
  end

  def toggle_quotes(str)
    if str.is_a?(String)
      return str[1...-1] if str.start_with?('"') && str.end_with?('"')

      return "\"#{str}\""
    end
    str
  end

  def stack_operate(op)
    case op
    when 'DROP' then @stack.pop
    when 'DUP' then @stack.push(@stack.last)
    when 'SWAP' then @stack[-1], @stack[-2] = @stack[-2], @stack[-1]
    when 'ROT' then @stack[-3], @stack[-2], @stack[-1] = @stack[-2], @stack[-1], @stack[-3]
    when 'ROLL', 'ROLLD'
      num = @stack.pop
      direction = op == 'ROLL' ? 1 : -1
      @stack[-num..-1] = @stack[-num..-1].rotate(direction)
    end
  end

  def conditional_operate(op)
    case op
    when 'IFELSE'
      condition = @stack.pop
      false_val = @stack.pop
      true_val = @stack.pop
      @stack.push(condition ? true_val : false_val)
    end
  end

  def eval_lambda(op)
    num_params, body = op[1..-2].split('|').map(&:strip)
    args = @stack.pop(num_params.to_i)
    body.gsub!(/x(\d+)/) { args[::Regexp.last_match(1).to_i].to_s }
    body.gsub!('SELF', "'#{op}")

    return_stack = @stack
    @stack = []
    interpret(body)
    return_stack.concat(@stack)
    @stack = return_stack
  end

  def parse_literal(op)
    if op.start_with?('"') && op.end_with?('"')
      @stack.push(op)
    else
      parsed_op = eval(op)
      case parsed_op
      when Integer, Float, TrueClass, FalseClass, String, Vector, Matrix
        @stack.push(parsed_op)
      when Array
        if parsed_op.any? { |el| el.is_a?(Array) }
          @stack.push(Matrix.rows(parsed_op))
        else
          @stack.push(Vector.elements(parsed_op))
        end
      end
    end
  end

  def interpret(input)
    input = [input] if input.is_a?(String)
    input.each do |line|
      split_ops(line).each do |op|
        perform_operation(op)
      end
    end
    @stack
  end

  def split_ops(line)
    ops = []
    current_op = ''
    square_brackets = 0
    curly_brackets = 0
    inside_quotes = false
    line.each_char do |char|
      case char
      when '[' then square_brackets += 1
      when ']' then square_brackets -= 1
      when '{' then curly_brackets += 1
      when '}' then curly_brackets -= 1
      when '"' then inside_quotes = !inside_quotes
      end
      if char == ' ' && square_brackets <= 0 && curly_brackets <= 0 && !inside_quotes
        ops << current_op unless current_op.empty?
        current_op = ''
      else
        current_op << char
      end
    end
    ops << current_op unless current_op.empty?
    ops
  end
end

begin
  input_file = ARGV[0]
  output_file = "output-#{input_file.match(/(\d{3})\.txt/)[1]}.txt"
  input_data = File.readlines(input_file).map(&:strip)

  interpreter = WackoStacko.new
  result = interpreter.interpret(input_data)

  File.open(output_file, 'w') do |f|
    result.each do |el|
      f.puts(el.is_a?(Vector) || el.is_a?(Matrix) ? el.to_a.inspect : el.to_s)
    end
  end
rescue StandardError => e
end
