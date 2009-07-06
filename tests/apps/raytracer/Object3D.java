public abstract class Object3D {
	Vector3D colour = new Vector3D(1,1,1);
	double reflectivity = 0;
	boolean fresnelReflection = false;
	
	public abstract HitResult intersect(Ray ray);
}
