package jszuru.resources;

import java.util.List;
import java.util.Map;

@SuppressWarnings("unused")
public class SzurubooruPostNote {
    protected List<Point> points;
    protected String text;

    @SuppressWarnings("unused")
    protected static class Point{
        protected float x;
        protected float y;

        protected Point(float x, float y){
            this.x = x;
            this.y = y;
        }

        public float getX() {
            return x;
        }
        public Point setX(float x) {
            this.x = x;
            return this;
        }

        public float getY() {
            return y;
        }
        public Point setY(float y) {
            this.y = y;
            return this;
        }
    }

    public SzurubooruPostNote(List<List<Float>> polygon, String text){
        this.points = polygon.stream().map(x -> new Point(x.get(0), x.get(1))).toList();
        this.text = text;
    }

    public List<Point> getPoints() {
        return points;
    }
    public String getText() {
        return text;
    }

    public Map<String, Object> json(){
        return Map.of("polygon", points.stream().map(point -> List.of(point.x, point.y)).toList(),
                "text", text);
    }
}
