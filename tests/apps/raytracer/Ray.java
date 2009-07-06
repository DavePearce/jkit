package raytracer;

public class Ray {

	public Vector3D origin, direction;

	public Ray(Vector3D origin, Vector3D direction)
	{
		this.origin = origin;
		this.direction = direction;
	}
}
