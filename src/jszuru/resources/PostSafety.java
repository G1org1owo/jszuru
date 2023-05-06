package jszuru.resources;

public enum PostSafety {
    SAFE,
    SKETCHY,
    UNSAFE;

    @Override
    public String toString(){
        return super.toString().toLowerCase();
    }

    public static PostSafety getEnum(String name){
        return PostSafety.valueOf(name.toUpperCase());
    }
}
