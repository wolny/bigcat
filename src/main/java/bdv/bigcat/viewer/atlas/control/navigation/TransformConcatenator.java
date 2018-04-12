package bdv.bigcat.viewer.atlas.control.navigation;

import java.lang.invoke.MethodHandles;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bdv.bigcat.viewer.state.GlobalTransformManager;
import net.imglib2.realtransform.AffineTransform3D;
import net.imglib2.ui.TransformListener;

public class TransformConcatenator
{

	private static final Logger LOG = LoggerFactory.getLogger( MethodHandles.lookup().lookupClass() );

	private final GlobalTransformManager manager;

	private final AffineTransform3D globalTransform = new AffineTransform3D();

	private final AffineTransformWithListeners global = new AffineTransformWithListeners( globalTransform );

	private final AffineTransformWithListeners concatenated = new AffineTransformWithListeners();

	private final AffineTransformWithListeners displayTransform;

	private final AffineTransformWithListeners globalToViewer;

	private TransformListener< AffineTransform3D > listener;

	private final TransformListener< AffineTransform3D > globalTransformTracker;

	private final Object lock;

	public TransformConcatenator(
			final GlobalTransformManager manager,
			final AffineTransformWithListeners displayTransform,
			final AffineTransformWithListeners globalToViewer,
			final Object lock )
	{
		super();
		this.manager = manager;
		this.displayTransform = displayTransform;
		this.globalToViewer = globalToViewer;
		this.globalTransformTracker = tf -> global.setTransform( tf );
		this.lock = lock;
		this.manager.addListener( this.globalTransformTracker );

		this.global.addListener( tf -> update() );
		this.displayTransform.addListener( tf -> update() );
		this.globalToViewer.addListener( tf -> update() );
		this.concatenated.addListener( tf -> notifyListener() );
	}

	private void notifyListener()
	{
		if ( listener != null )
			listener.transformChanged( concatenated.getTransformCopy() );
	}

	private void update()
	{
		synchronized ( lock )
		{
			final AffineTransform3D concatenated = new AffineTransform3D();
			LOG.warn( "Concatenating: {} {} {}", displayTransform, globalToViewer, globalTransform );
			concatenated.set( globalTransform );
			concatenated.preConcatenate( globalToViewer.getTransformCopy() );
			concatenated.preConcatenate( displayTransform.getTransformCopy() );
			this.concatenated.setTransform( concatenated );
		}
	}

	public synchronized AffineTransform3D getTransform()
	{
		return concatenated.getTransformCopy();
	}

	public void setTransformListener( final TransformListener< AffineTransform3D > transformListener )
	{
		this.listener = transformListener;
		notifyListener();
	}

}