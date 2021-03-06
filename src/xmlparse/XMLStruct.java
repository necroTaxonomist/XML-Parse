package xmlparse;

import java.util.ArrayList;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;

public class XMLStruct implements Cloneable
{
    private ArrayList<Child> children;
    private String type;
    private ArrayList<Attribute> attribs;
    private boolean keepWS;

    // Constructors

    public XMLStruct()
    {
        children = new ArrayList<Child>();
        type = "";
        attribs = new ArrayList<Attribute>();
        keepWS = true;
    }

    public XMLStruct(String name)
    {
        this();
        type = name;
    }

    public XMLStruct(String name, String ... attribList)
    {
        this(name);

        for (int i = 0; i + 1 < attribList.length; i += 2)
        {
            addAttribute(attribList[i], attribList[i+1]);
        }
    }

    public XMLStruct(XMLStruct xml)
    {
        this();

        for (Child c : xml.children)
        {
            children.add(c);
        }

        type = xml.type;

        for (Attribute a : xml.attribs)
        {
            attribs.add(a);
        }

        keepWS = xml.keepWS;
    }

    public Object clone()
    {
        return new XMLStruct(this);
    }

    public static XMLStruct parseFromString(String str) throws BadSyntaxException
    {
        CStream<String> stream = new CStream<String>();
        stream.add(str);
        return parseFromStream(stream);
    }

    public static XMLStruct parseFromStream(CStream<String> stream) throws BadSyntaxException
    {
        XMLStruct xml = new XMLStruct();
        xml.parse(stream);
        return xml;
    }

    public static XMLStruct parseFromFile(String fn) throws BadSyntaxException
    {
        return parseFromFile(fn, false);
    }
    public static XMLStruct parseFromFile(String fn, boolean keepWS) throws BadSyntaxException
    {
        CStream<String> stream = new CStream<String>();

        FileInputStream fis = null;
        BufferedReader br = null;
        XMLStruct xml = null;
        try
        {
            fis = new FileInputStream(fn);
        	br = new BufferedReader(new InputStreamReader(fis));

        	String l = null;
        	while ((l = br.readLine()) != null)
            {
        		stream.add(l + "\n");
        	}

            xml = new XMLStruct();
            xml.keepWS = keepWS;
            xml.parse(stream);
        }
        catch (IOException e)
        {
        }
        finally
    	{
            try
            {
                if (br != null)
    		        br.close();
                if (fis != null)
        		    fis.close();
    	    }
            catch(Exception ex)
            {
    	    }
    	}

        return xml;
    }

    public String saveToFile(String fn)
    {
        return saveToFile(fn, false);
    }
    public String saveToFile(String fn, boolean addNL)
    {
        String contents = null;
        if (addNL)
            contents = toStringNL();
        else
            contents = toString();

        BufferedWriter bw = null;
        try
        {
            File file = new File(fn);

    	    if (!file.exists())
                file.createNewFile();

    	    FileWriter fw = new FileWriter(file);
    	    bw = new BufferedWriter(fw);

    	    bw.write(contents);
        }
        catch (IOException ioe)
        {
            contents = null;
    	    System.out.println("Failed to write XML file");
    	}
    	finally
    	{
            try
            {
                if (bw != null)
    		        bw.close();
    	    }
            catch(Exception ex)
            {
    	    }
    	}

        if (contents == null)
            return "";
        else
            return contents;
    }

    // Public getters

    public int getNumChildren()
    {
        return children.size();
    }

    public XMLStruct getChildElement(int index)
    {
        if (index < 0 || index >= children.size())
            return null;

        Child c = children.get(index);
        return c.struct;
    }

    public XMLStruct getChildElement(String name)
    {
        for (Child c : children)
        {
            if (c.struct != null &&
                c.struct.type.equals(name))
                return c.struct;
        }

        return null;
    }

    public String getChildString(int index)
    {
        if (index < 0 || index >= children.size())
            return null;

        Child c = children.get(index);
        return c.string;
    }

    public String getChildString()
    {
        return getChildString(0);
    }

    public String getName()
    {
        return type;
    }

    public int getNumAttribs()
    {
        return attribs.size();
    }

    public String getAttribNameFromIndex(int index)
    {
        if (index < 0 || index >= attribs.size())
            return null;

        Attribute a = attribs.get(index);
        return a.name;
    }

    public String getAttribValueFromIndex(int index)
    {
        if (index < 0 || index >= attribs.size())
            return null;

        Attribute a = attribs.get(index);
        return a.val;
    }

    public String getAttribValueFromName(String str)
    {
        for (int i = 0; i < attribs.size(); ++i)
        {
            Attribute a = attribs.get(i);
            if (a.name.equals(str))
                return a.val;
        }

        return null;
    }

    public String toString()
    {
        String str = "";

        str += "<";
        str += type;
        for (Attribute a : attribs)
            str += " " + a;
        str += ">";

        for (Child c : children)
        {
            str += c;
        }

        str += "</" + type + ">";

        return str;
    }

    public String toStringNL()
    {
        String str = "";

        str += "<";
        str += type;
        for (Attribute a : attribs)
            str += " " + a;
        str += ">";

        boolean prevNL = false;
        for (Child c : children)
        {
            String line = children.get(0).string;

            if (!prevNL)
            {
                if (line == null ||
                    (line.length() > 0 && line.charAt(0) != '\n'))
                {
                    str += "\n\r";
                }
            }

            if (line != null &&
                line.length() > 0 &&
                (line.charAt(line.length()-1) == '\n' ||
                line.charAt(line.length()-1) == '\r'))
            {
                prevNL = true;
            }
            else
                prevNL = false;

            str += c.toStringNL();
        }

        if (!prevNL)
            str += "\n\r";

        str += "</" + type + ">";

        return str;
    }

    // Public setters
    public XMLStruct addChild(XMLStruct xml)
    {
        children.add(new Child(xml));
        return this;
    }

    public XMLStruct addChild(String str)
    {
        children.add(new Child(str));
        return this;
    }

    public XMLStruct addAttribute(String name, String value)
    {
        attribs.add(new Attribute(name, value));
        return this;
    }

    // Private

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

        boolean[] commented = {false};

        while ((str = stream.peek()) != null)
        {
            str = cutComments(str, commented);
            stream.switchFront(str);

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
                                if (keepWS)
                                    children.add(new Child(contents));
                                else if (!onlyWS(contents))
                                    children.add(new Child(contents.trim()));
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
                            nest.keepWS = keepWS;

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
        i = 0;

        // Find the type of the tag
        j = skipWS(str, i);
        i = skipUntil(str, j, WHITESPACE, null);
        type = evalBS(str.substring(j, i));

        if (!validXMLName(type))
            throw new BadSyntaxException("Invalid element name: " + type);

        // Get attributes
        while ((j = skipWS(str, i)) < str.length())
        {
            // Get the name of the attribute
            String name;

            i = skipUntil(str, j, WHITESPACE_OR_EQ, STRING_ESCAPES);
            name = evalBS(str.substring(j, i));

            if (name.length() == 0)
                throw new BadSyntaxException("Attribute without name: <" + str + ">");

            if (!validXMLName(name))
                throw new BadSyntaxException("Invalid attribute name: " + name);

            // Get equals sign
            j = skipWS(str, i);

            if (j >= str.length() || str.charAt(j) != '=')
                throw new BadSyntaxException("Attribute must be assigned with equals: <" + str + ">");

            i = j + 1;

            // Get open quote
            j = skipWS(str, i);

            if (j >= str.length() || str.charAt(j) != '\"' || str.charAt(j) != '\"')
                throw new BadSyntaxException("Attribute value must be enclosed in quotes: <" + str + ">");

            i = j + 1;

            // Get the value of the attribute
            String val;

            j = i;
            char[] close = {'\"', '\''};
            i = skipUntil(str, j, close, STRING_ESCAPES);
            val = evalBS(str.substring(j, i));

            // Get the close quote
            if (i >= str.length() || str.charAt(i) != '\"')
                throw new BadSyntaxException("Attribute value must be enclosed in quotes: <" + str + ">");

            i = i + 1;

            // Add to attribs list
            attribs.add(new Attribute(name, val));
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

    private String cutComments(String str, boolean[] commented)
    {
        int i = 0;
        int j = i;

        String ret = "";

        while (i < str.length())
        {
            if (!commented[0])
            {
                i = str.indexOf("<!--", j);
                if (i < 0)
                {
                    ret += str.substring(j);
                    break;
                }
                else
                {
                    ret += str.substring(j, i);
                    i += 4;
                    commented[0] = true;
                }
            }
            else
            {
                j = str.indexOf("-->", i);
                if (j < 0)
                {
                    break;
                }
                else
                {
                    j += 3;
                    commented[0] = false;
                }
            }
        }

        return ret;
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
                    case '\"':
                    case '\'':
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

    private static boolean validXMLName(String str)
    {
        if (str.length() == 0)
            return false;

        char c = str.charAt(0);

        // Element names must start with a letter or underscore
        if (!Character.isLetter(c) && c != '_')
            return false;

        // Element names cannot start with the letters xml (or XML, or Xml, etc)
        if (str.toLowerCase().indexOf("xml") == 0)
            return false;

        for (int i = 1; i < str.length(); ++i)
        {
            c = str.charAt(i);

            // Element names can contain letters, digits, hyphens, underscores, and periods
            if (!Character.isLetter(c) &&
                !Character.isDigit(c) &&
                c != '-' &&
                c != '_' &&
                c != '.')
            {
                return false;
            }
        }

        return true;
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

        public String toStringNL()
        {
            if (which == 0)
                return struct.toStringNL();
            else
                return string;
        }
    }

    private static class Attribute
    {
        public String name;
        public String val;

        public Attribute(String _name, String _val)
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
            XMLStruct xml = parseFromFile("test.xml", true);
            //XMLStruct xml = parseFromString("<hello></hello>");
            System.out.println(xml);
        }
        catch (Exception e)
        {

        }
    }
}
