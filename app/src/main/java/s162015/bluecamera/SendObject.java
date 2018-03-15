package s162015.bluecamera;

import java.io.Serializable;

/**
 * Created by s162015 on 2017/01/13.
 */

public class SendObject implements Serializable {
    public enum Type{
        message,preview,picture
    }
    public Type type;
    public Object data;
    public SendObject(Type type,Object data){
        this.type = type;
        this.data = data;
    }
}