package celtech.gcodeviewer.gcode;

import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.parboiled.Action;
import org.parboiled.BaseParser;
import org.parboiled.Context;
import org.parboiled.Rule;
import org.parboiled.annotations.SuppressSubnodes;
import org.parboiled.support.Var;

/**
 *
 * @author Tony Aldhous
 */
//@BuildParseTree
public class GCodeLineParser extends BaseParser<GCodeLine>
{
    private final Stenographer steno = StenographerFactory.getStenographer(GCodeLineParser.class.getName());

    private final GCodeLine line = new GCodeLine();
    
    public GCodeLine getLine()
    {
        return line;
    }

    public void resetLine()
    {
        line.reset();
    }

    public Rule Line()
    {
        return FirstOf(
            Command(),
            TypeComment(),
            LayerComment(),
            Comment(),
            EqualsComment()
        );
    }
    
    // A M or G command e.g. G0 F12000 X88.302 Y42.421 Z1.020
    @SuppressSubnodes
    Rule Command()
    {
        Var<Character> commandLetterValue = new Var<>();
        Var<Integer> commandNumberValue = new Var<>();
        Var<Character> valKey = new Var<>();
        Var<Double> valValue = new Var<>();

        return Sequence(
                    FirstOf('G', 'M', 'T'),
                    commandLetterValue.set(match().charAt(0)),
                    OneOrMore(Digit()),
                    commandNumberValue.set(Integer.valueOf(match())),
                    Optional(' '),
                    ZeroOrMore(
                        Sequence(
                            CharRange('A', 'Z'),
                            valKey.set(match().charAt(0)),
                            Optional(
                                Sequence(
                                    FloatingPointNumber(),
                                    valValue.set(Double.valueOf(match()))
                                )
                            ),
                            Optional(' '),
                            new Action()
                            {
                                @Override
                                public boolean run(Context context)
                                {
                                    if (valValue.isSet())
                                        line.setValue(valKey.get(), valValue.get());
                                    else
                                        line.setValue(valKey.get(), Double.NaN);
                                    return true;
                                }
                            }
                        )
                    ),
                    Optional(
                        Comment()
                    ),
                    new Action()
                    {
                        @Override
                        public boolean run(Context context)
                        {
                            line.commandLetter = commandLetterValue.get();
                            line.commandNumber = commandNumberValue.get();
                            return true;
                        }
                    }
                );
    }
    
    // Comment specifying a type.
    // ;TYPE:FILL\n
    @SuppressSubnodes
    Rule TypeComment()
    {
        return Sequence(
                    ZeroOrMore(' '),
                    IgnoreCase(";TYPE:"),
                    OneOrMore(
                        FirstOf(
                            CharRange('a', 'z'),
                            CharRange('A', 'Z'),
                            '-'
                        )
                    ),
                    new Action()
                    {
                        @Override
                        public boolean run(Context context)
                        {
                            line.type = match().trim();
                            return true;
                        }
                    }
                );
    }

    // Comment specifying a layer.
    // ;LAYER:34 height:1.47
    @SuppressSubnodes
    Rule LayerComment()
    {
        Var<Integer> layerValue = new Var<>();
        Var<Double> heightValue = new Var<>();

        return Sequence(
                    ZeroOrMore(' '),
                    ';',
                    ZeroOrMore(' '),
                    IgnoreCase("LAYER"),
                    ZeroOrMore(' '),
                    Optional(':'),
                    ZeroOrMore(' '),
                    OneOrMore(Digit()),
                    layerValue.set(Integer.valueOf(match())),
                    ZeroOrMore(' '),
                    Optional(
                        Sequence(
                            IgnoreCase("HEIGHT"),
                            ZeroOrMore(' '),
                            Optional(':'),
                            ZeroOrMore(' '),
                            PositiveFloatingPointNumber(),
                            heightValue.set(Double.valueOf(match()))
                        )
                    ),
                    ZeroOrMore(ANY),
                    new Action()
                    {
                        @Override
                        public boolean run(Context context)
                        {
                            line.layerNumber = layerValue.get();
                            if (heightValue.isSet())
                                line.layerHeight = heightValue.get();
                            line.comment = match().trim();
                            return true;
                        }
                    }                    
                );
    }
    
    // Comment element.
    // ;Blah blah blah\n
    @SuppressSubnodes
    Rule Comment()
    {
        return Sequence(
                ZeroOrMore(' '),
                ';',
                ZeroOrMore(ANY),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        line.comment = match().trim();
                        return true;
                    }
                }
        );
    }
    
    // Comment element.
    // ;Blah blah blah\n
    @SuppressSubnodes
    Rule EqualsComment()
    {
        return Sequence(
                ZeroOrMore(' '),
                '=',
                ZeroOrMore(ANY),
                new Action()
                {
                    @Override
                    public boolean run(Context context)
                    {
                        line.comment = match().trim();
                        return true;
                    }
                }
        );
    }

    @SuppressSubnodes
    Rule Digit()
    {
        return CharRange('0', '9');
    }

    @SuppressSubnodes
    Rule FloatingPointNumber()
    {
        return Sequence(
                    Optional(
                        FirstOf(
                            Ch('+'),
                            Ch('-')
                        )
                    ),
                    UnsignedFloatingPointNumber()
                );
    }
    
    @SuppressSubnodes
    Rule UnsignedFloatingPointNumber()
    {
        //Positive double e.g. 1.23
        return Sequence(
                    OneOrMore(Digit()),
                    Optional(
                        Sequence(
                            Ch('.'),
                            OneOrMore(Digit())
                        )
                    )
                );
    }

    @SuppressSubnodes
    Rule PositiveFloatingPointNumber()
    {
        //Positive double e.g. 1.23
        return Sequence(
                Optional(Ch('+')),
                UnsignedFloatingPointNumber());
    }

    @SuppressSubnodes
    Rule NegativeFloatingPointNumber()
    {
        //Negative double e.g. -1.23
        return Sequence(
                Ch('-'),
                UnsignedFloatingPointNumber());
    }
}
