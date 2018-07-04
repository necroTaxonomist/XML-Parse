import java.util.ArrayList;

import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class XMLStruct
{
    ArrayList<Child> children;
    String type;
    ArrayList<Argument> args;
    boolean keepWS;

    public XMLStruct()
    {
        children = new ArrayList<Child>();
        type = "";
        args = new ArrayList<Argument>();
        keepWS = true;
    }

    public XMLStruct(String str) throws BadSyntaxException
    {
        this();
        CStream<String> stream = new CStream<String>();
        stream.add(str);
        parse(stream);
    }

    public XMLStruct(CStream<String> stream) throws BadSyntaxException
    {
        this();
        parse(stream);
    }

    public static XMLStruct parseFromFile(String fn) throws BadSyntaxException, IOException
    {
        return parseFromFile(fn, true);
    }
    public static XMLStruct parseFromFile(String fn, boolean keepWS) throws BadSyntaxException, IOException
    {
        CStream<String> stream = new CStream<String>();

        FileInputStream fis = new FileInputStream(fn);
    	BufferedReader br = new BufferedReader(new InputStreamReader(fis));

    	String l = null;
    	while ((l = br.readLine()) != null)
        {
    		stream.add(l + "\n");
    	}

        XMLStruct xml = new XMLStruct();
        xml.keepWS = keepWS;
        xml.parse(stream);

        return xml;
    }

    private int parse(CStream<String> stream) throws BadSyntaxException
    {
        return parse(stream, 0);
    }
    private int parse(CStream<String> stream, int i) throws BadSyntaxException
    {
        String prevStr = "";
        ParseState state = ParseState.PARSE_BEGIN;

        boolean openTag = false;
        boolean closeTag = false;
        String contents = "";

        String str = "";
        String pastStr = "";

        while ((str = stream.peek()) != null)
        {
            int j = i;

            while (i < str.length())
            {
                switch (state)
                {
                    case PARSE_BEGIN:
                        j = i;
                        char[] open = {'<'};
                        i = skipUntil(str, i, open, null);

                        if (i < str.length())
                        {
                            contents = prevStr + str.substring(j, i);
                            prevStr = "";
                            if (contents.length() > 0)
                            {
                                if (keepWS || !onlyWS(contents))
                                    children.add(new Child(contents));
                            }

                            j = i;
                            ++i;
                            state = ParseState.PARSE_CLOSE;
                        }
                        else
                        {
                            prevStr += str.substring(j, i);
                        }
                        break;
                    case PARSE_CLOSE:
                        i = skipWS(str, i);
                        if (str.charAt(i) == '/')
                        {
                            if (!openTag)
                            {
                                throw new BadSyntaxException("Close tag without matching open: " + str);
                            }
                            closeTag = true;
                            ++i;
                            state = ParseState.PARSE_END;
                        }
                        else if (openTag)  // if this struct already has an open tag
                        {
                            // Create nested
                            XMLStruct nest = new XMLStruct();

                            i = nest.parse(stream, j);
                            str = stream.peek();

                            children.add(new Child(nest));

                            state = ParseState.PARSE_BEGIN;
                        }
                        else
                        {
                            state = ParseState.PARSE_END;
                        }
                        break;
                    case PARSE_END:
                        j = i;
                        i = skipUntil(str, i, CLOSERS, STRING_ESCAPES);

                        if (i < str.length())
                        {
                            contents = prevStr + str.substring(j, i);
                            prevStr = "";

                            if (str.charAt(i) == '/')
                            {
                                if (!closeTag)
                                {
                                    closeTag = true;
                                    parseOpen(contents);

                                    i = skipWS(str, i + 1);

                                    if (str.charAt(i) != '>')
                                    {
                                        throw new BadSyntaxException("Improper self-closing tag: " + str);
                                    }
                                }
                                else
                                {
                                    throw new BadSyntaxException("Close tag marked self-closing: " + str);
                                }
                            }
                            else
                            {
                                if (!closeTag)
                                {
                                    parseOpen(contents);
                                }
                                else
                                {
                                    parseClose(contents);
                                }
                            }

                            ++i;
                            if (closeTag)
                            {
                                 return i;
                            }
                            else
                            {
                                openTag = true;
                            }
                            state = ParseState.PARSE_BEGIN;
                        }
                        else
                        {
                            prevStr += str.substring(j, i);
                        }
                        break;
                    default:
                        throw new BadSyntaxException("Invalid parser state");
                }
            }

            i = 0;
            pastStr = stream.get();
        }

        if (!closeTag)
        {
            throw new BadSyntaxException("Open tag without matching close: " + pastStr);
        }

        return str.length();
    }

    private void parseOpen(String str) throws BadSyntaxException
    {
        int j, i;

        // Find the type of the tag
        j = skipWS(str, 0);
        i = skipUntil(str, j, WHITESPACE, null);
        type = evalBS(str.substring(j, i));

        // Get arguments
        while ((j = skipWS(str, i)) < str.length())
        {
            // Get the name of the argument
            String name;

            i = skipUntil(str, j, WHITESPACE_OR_EQ, STRING_ESCAPES);
            name = evalBS(str.substring(j, i));

            if (name.length() == 0)
                throw new BadSyntaxException("Argument without name: <" + str + ">");

            // Get equals sign
            j = skipWS(str, i);

            if (j >= str.length() || str.charAt(j) != '=')
                throw new BadSyntaxException("Argument must be assigned with equals: <" + str + ">");

            i = j + 1;

            // Get open quote
            j = skipWS(str, i);

            if (j >= str.length() || str.charAt(j) != '\"')
                throw new BadSyntaxException("Argument value must be enclosed in quotes: <" + str + ">");

            i = j + 1;

            // Get the value of the argument
            String val;

            j = i;
            char[] close = {'\"'};
            i = skipUntil(str, j, close, STRING_ESCAPES);
            val = evalBS(str.substring(j, i));

            // Get the close quote
            if (i >= str.length() || str.charAt(i) != '\"')
                throw new BadSyntaxException("Argument value must be enclosed in quotes: <" + str + ">");

            i = i + 1;

            // Add to args list
            args.add(new Argument(name, val));
        }
    }

    private void parseClose(String str) throws BadSyntaxException
    {
        int j, i;

        // Find the type of the tag
        j = skipWS(str, 0);
        i = skipUntil(str, j, WHITESPACE, null);
        String closeType = evalBS(str.substring(j, i));

        if (!closeType.equals(type))
        {
            throw new BadSyntaxException("Close tag without matching open: </" + str + ">");
        }

        i = skipWS(str, i);
        if (i != str.length())
        {
            throw new BadSyntaxException("Invalid close tag: </" + str + ">");
        }
    }

    public XMLStruct getChildTag(int index)
    {
        if (index < 0 || index >= children.size())
            return null;

        Child c = children.get(index);
        return c.struct;
    }

    public String getChildString(int index)
    {
        if (index < 0 || index >= children.size())
            return null;

        Child c = children.get(index);
        return c.string;
    }

    public String toString()
    {
        String str = "";

        str += "<";
        str += type;
        for (Argument arg : args)
            str += " " + arg;
        str += ">";

        for (Child c : children)
        {
            str += c;
        }

        str += "</" + type + ">";

        return str;
    }

    // Static

    private enum ParseState
    {
        PARSE_BEGIN,  // < of a tag
        PARSE_CLOSE, // look for / marking a close tag
        PARSE_OPEN_CONTENTS,  // contents of a tag
        PARSE_END  // > of a tag
    }

    private static final char[] STRING_ESCAPES = {'"', '\''};
    private static final char[] WHITESPACE = {' ', '\n', '\r', '\t'};
    private static final char[] WHITESPACE_OR_EQ = {' ', '\n', '\r', '\t', '='};
    private static final char[] CLOSERS = {'>', '/'};

    private static int skipWS(String str, int i)
    {
        for (; i < str.length(); ++i)
        {
            char c = str.charAt(i);

            switch (c)
            {
                case ' ':
                case '\n':
                case '\r':
                case '\t':
                    break;
                default:
                    return i;
            }
        }

        return i;
    }

    private static int skipUntil(String str, int i, char[] look, char[] escapes) throws BadSyntaxException
    {
        if (look == null)
            return str.length();

        boolean backslash = false;

        for (; i < str.length(); ++i)
        {
            char c = str.charAt(i);

            if (!backslash)
            {
                for (int j = 0; j < look.length; ++j)
                {
                    if (c == look[j])
                        return i;
                }

                if (c == '\\')
                {
                    backslash = true;
                }
                else if (escapes != null)
                {
                    for (int j = 0; j < escapes.length; ++j)
                    {
                        if (c == escapes[j])
                        {
                            char[] close = {escapes[j]};
                            i = skipUntil(str, ++i, close, escapes);
                        }
                    }
                }
            }
            else
                backslash = false;
        }

        return i;
    }

    private String evalBS(String str) throws BadSyntaxException
    {
        boolean backspace = false;

        for (int i = 0; i < str.length(); ++i)
        {
            char c = str.charAt(i);

            if (!backspace && c == '\\')
            {
                backspace = true;
                str = str.substring(0, i) + str.substring(i + 1);
                --i;
            }
            else if (backspace)
            {
                switch (c)
                {
                    case 'n':
                        str = str.substring(0, i) + "\n" + str.substring(i + 1);
                        break;
                    case 't':
                        str = str.substring(0, i) + "\t" + str.substring(i + 1);
                        break;
                    case '\\':
                        str = str.substring(0, i) + "\\" + str.substring(i + 1);
                        break;
                    case '>':
                    case '\"':
                    case '\'':
                    case '=':
                        str = str.substring(0, i) + c + str.substring(i + 1);
                        break;
                    case 'r':
                        str = str.substring(0, i) + str.substring(i + 1);
                        --i;
                        break;
                    default:
                        throw new BadSyntaxException("Unrecognized escape sequence: " + str +
                                                     ", col " + i);
                }
                backspace = false;
            }
        }

        return str;
    }

    private static boolean onlyWS(String str)
    {
        return skipWS(str, 0) == str.length();
    }

    public static class BadSyntaxException extends Exception
    {
        public BadSyntaxException(String context)
        {
            super(context);
        }
    }

    private static class Child
    {
        public XMLStruct struct;
        public String string;
        public int which;

        public Child(XMLStruct _struct)
        {
            struct = _struct;
            string = null;
            which = 0;
        }

        public Child(String _string)
        {
            struct = null;
            string = _string;
            which = 1;
        }

        public String toString()
        {
            if (which == 0)
                return "" + struct;
            else
                return string;
        }
    }

    private static class Argument
    {
        public String name;
        public String val;

        public Argument(String _name, String _val)
        {
            name = _name;
            val = _val;
        }

        public String toString()
        {
            return name + "=\"" + val + "\"";
        }
    }

    public static void main(String[] args)
    {
        try
        {
            XMLStruct xml = parseFromFile("test.xml", false);

            System.out.println(xml);
        }
        catch (Exception e)
        {
            System.out.println(e);
        }
    }
}
