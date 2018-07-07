package xmlparse;

import java.util.stream.Stream;
import java.util.LinkedList;

// C++ style stream
public class CStream<T>
{
    private LinkedList<T> myList;

    public CStream()
    {
        myList = new LinkedList<T>();
    }

    public CStream(Stream<T> stream)
    {
        this();

        stream.forEach((t)->
            {
                add(t);
            });
    }

    public CStream<T> add(T t)
    {
        myList.addLast(t);
        return this;
    }

    public T get()
    {
        try
        {
            return myList.removeFirst();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public T peek()
    {
        return myList.peekFirst();
    }

}
