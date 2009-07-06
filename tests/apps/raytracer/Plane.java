public class Plane extends Object3D {
	Vector3D center = new Vector3D(0,0,0);
	Vector3D normal = new Vector3D(0,1,0);
	
	public Plane(Vector3D center, Vector3D normal)
	{
		this.center = center;
		this.normal = normal;
	}
	
	public HitResult intersect(Ray ray)
	{
		HitResult r = new HitResult();
		Vector3D n = normal.clone();
		
		if(ray.direction.dot(normal) == 0) 
		{
			r.hit = false;
			return r;
		}
		else if(ray.direction.dot(normal) > 0)
		{
			n.multiply(-1);
		}
		
		double d = -(ray.origin.dot(normal) - normal.dot(center))/ray.direction.dot(normal);
		
		if(d < 0.0000001)
		{
			r.hit = false;
			return r;
		}
		
		r.distance = d;
		r.normal = n;
		r.hit = true;
		r.intersection = ray.origin.plus(ray.direction.times(d));

	    return r;
	}
}
