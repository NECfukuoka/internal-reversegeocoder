package gsi.cyberjapan.reversegeocoder;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.geotools.data.FeatureSource;
import org.geotools.data.FileDataStoreFactorySpi;
import org.geotools.data.shapefile.ShapefileDataStore;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.json.simple.JSONObject;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

/**
 * リバースジオコーダ
 *
 */
public class ReverseGeocoderServlet extends HttpServlet {
	/**
	 * serialVersionUID
	 */
	private static final long serialVersionUID = 992215912721577402L;
	/**
	 * リバースジオコーダDataSource
	 */
	private ShapefileDataStore dataStore;
	/* (non-Javadoc)
	 * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		String dataFileName = this.getInitParameter("data");
		if (dataFileName.isEmpty()) {
			throw new ServletException("Fail to read data parameter.");
		}
		dataFileName = this.getServletContext().getRealPath(dataFileName);
		if (!new File(dataFileName).exists()) {
			throw new ServletException("Fail to read " + this.getInitParameter("data") + " is not exists.");
		}
		try {
			FileDataStoreFactorySpi factory = new ShapefileDataStoreFactory();
			Map<String,Serializable> params = new HashMap<String,Serializable>();
			params.put("url", new URL("file://" +dataFileName));
			params.put("charset", "Shift_JIS");
	
			this.dataStore = (ShapefileDataStore)factory.createDataStore( params );
		} catch (Exception e) {
			throw new ServletException(e.getMessage(),e);
		}
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		JSONObject result = new JSONObject();
		List<String> errors = new ArrayList<String>();
		if (StringUtils.isEmpty(req.getParameter("lat"))) {
			errors.add("lat required.");
		} else if (StringUtils.isNumeric(req.getParameter("lat"))) {
			errors.add("lat must be a number.");
		}
		if (StringUtils.isEmpty(req.getParameter("lon"))) {
			errors.add("lon required.");
		} else if (StringUtils.isNumeric(req.getParameter("lon"))) {
			errors.add("lon must be a number.");
		}
		if (errors.size() == 0) {
			@SuppressWarnings("rawtypes")
			FeatureSource featureSource = this.dataStore.getFeatureSource();
			FilterFactory2 filterFactory = CommonFactoryFinder.getFilterFactory2(null);
			double x = Double.parseDouble(req.getParameter("lon"));
			double y = Double.parseDouble(req.getParameter("lat"));
			Point p = new GeometryFactory().createPoint(new Coordinate(x, y));
			Filter filter = filterFactory.intersects(filterFactory.property(featureSource.getSchema().getGeometryDescriptor().getLocalName()), filterFactory.literal(p));
			FeatureCollection<SimpleFeatureType, SimpleFeature> selectedFeatures = featureSource.getFeatures(filter);
			FeatureIterator<SimpleFeature> iter = selectedFeatures.features();
			try {
				if (iter.hasNext()) {
					// 一点のみを返却
					SimpleFeature feature = iter.next();
					JSONObject record = new JSONObject();
					String muniCd = (String)feature.getProperty("行政コード").getValue();
					String lv01Nm = (String)feature.getProperty("LV01").getValue();
					if (!StringUtils.isEmpty(lv01Nm)) {
						record.put("muniCd",!StringUtils.isEmpty(muniCd)?muniCd:"");
						record.put("lv01Nm",!StringUtils.isEmpty(lv01Nm)?lv01Nm:"");
						result.put("results",record);
					}
				}
			} finally {
				iter.close();
			}
		}
		resp.setContentType("application/json; charset=utf-8");
		resp.addHeader("Access-Control-Allow-Origin", "*");
		String resultData = result.toJSONString();
		byte[] buff = resultData.getBytes(Charset.forName("UTF-8"));
		resp.getOutputStream().write(buff);
	}
}
