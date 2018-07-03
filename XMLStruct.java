import java.util.ArrayList;

public class XMLStruct
{
    ArrayList<Child> children;

    public XMLStruct()
    {
        children = new ArrayList<Child>();
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
                                children.add(new Child(contents));

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
                        char[] close = {'>'};
                        i = skipUntil(str, i, close, STRING_ESCAPES);

                        if (i < str.length())
                        {
                            contents = prevStr + str.substring(j, i);
                            prevStr = "";
                            // process tag contents

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
            stream.get();
        }

        return str.length();
    }

    private enum ParseState
    {
        PARSE_BEGIN,  // < of a tag
        PARSE_CLOSE, // look for / marking a close tag
        PARSE_OPEN_CONTENTS,  // contents of a tag
        PARSE_END  // > of a tag
    }

    private static final char[] STRING_ESCAPES = {'"', '\''};

    private static int skipWS(String str, int i) throws BadSyntaxException
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

    public String toString()
    {
        String str = "";

        str += "<TAG>";

        for (Child c : children)
        {
            if (c.which == 0)
                str += c.struct;
            else
                str += c.string;
        }

        str += "</TAG>";

        return str;
    }

    public static class BadSyntaxException extends Exception
    {
        public BadSyntaxException(String context)
        {
            super("Bad syntax: " + context + ".");
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
    }

    public static void main(String[] args)
    {
        try
        {
            CStream<String> stream = new CStream<String>();

            stream.add("<hello> \n");
            stream.add("fdfdfdf<goodbye>\n");
            stream.add("fdfdfd\nfdfdfdf\n");
            stream.add("</goodbye>qqqq\n");
            stream.add("<hey>YO</hey>\n");
            stream.add("</hello>");

            XMLStruct xml = new XMLStruct(stream);

            System.out.println(xml);
        }
        catch (BadSyntaxException e)
        {
            System.out.println(e);
        }
    }
}
