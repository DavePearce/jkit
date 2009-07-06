package raytracer;

public class Sphere extends Object3D {
	Vector3D center = new Vector3D(0,0,0);
	double radius;
	
	public Sphere(Vector3D center, double radius)
	{
		this.center = center;
		this.radius = radius;
	}
	
	public HitResult intersect(Ray ray)
	{
		HitResult result = new HitResult();
		
		Vector3D d = center.minus(ray.origin);
		double v = ray.direction.dot(d);

	    // Test if the ray actually intersects the sphere
		double t = radius*radius + v*v - d.x*d.x - d.y*d.y - d.z*d.z;
		
	    if(t < 0.0)
	    	return result;

	    // Test if the intersection is in the positive ray direction
	    t = v - Math.sqrt(t);
	    
	    if(t < 0.0)
	    	return result;
	    
	    result.hit = true;
	    result.distance = t;
	    
	    Vector3D intersect = ray.origin.plus(ray.direction.times(t));
		result.normal = intersect.minus(center);
		result.normal.multiply(1.0/radius);
		result.intersection = intersect;

	    return result;
	}
}
