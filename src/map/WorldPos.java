package map;

public record WorldPos(float x, float y) {

    public double angleTo(WorldPos pos) {
        float dx = x() - pos.x();
        float dy = y() - pos.y();
        return Math.atan2(dy, dx);
    }

}
