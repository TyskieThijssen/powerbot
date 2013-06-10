package org.powerbot.script.wrappers;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;

import org.powerbot.script.methods.ClientFactory;
import org.powerbot.client.ModelCapture;
import org.powerbot.script.methods.Game;
import org.powerbot.script.methods.Widgets;
import org.powerbot.script.methods.widgets.ActionBar;
import org.powerbot.script.util.Random;

public abstract class Model {
	protected final int[] yPoints;
	protected final short[] faceA;
	protected final short[] faceB;
	protected final short[] faceC;
	protected final int numFaces;
	protected final int numVertices;
	protected int[] xPoints;
	protected int[] zPoints;

	public Model(final org.powerbot.client.Model model) {
		xPoints = model.getXPoints();
		yPoints = model.getYPoints();
		zPoints = model.getZPoints();
		faceA = model.getIndices1();
		faceB = model.getIndices2();
		faceC = model.getIndices3();

		if (model instanceof ModelCapture) {
			numVertices = ((ModelCapture) model).getNumVertices();
			numFaces = ((ModelCapture) model).getNumFaces();
		} else {
			numVertices = Math.min(xPoints.length, Math.min(yPoints.length, zPoints.length));
			numFaces = Math.min(faceA.length, Math.min(faceB.length, faceC.length));
		}
	}

	public abstract int getX();

	public abstract int getY();

	public abstract byte getPlane();

	public abstract void update();

	public int nextTriangle() {
		update();
		final int mark = Random.nextInt(0, numFaces);
		int index = firstOnScreenIndex(mark, numFaces);
		return index != -1 ? index : firstOnScreenIndex(0, mark);
	}

	public Point getCentroid(final int index) {
		if (index < 0 || index >= numFaces) return null;
		update();
		final int x = getX();
		final int y = getY();
		final int plane = getPlane();
		final int height = Game.tileHeight(x, y, plane);
		final Point localPoint = Game.worldToScreen(
				x + (this.xPoints[this.faceA[index]] + this.xPoints[this.faceB[index]] + this.xPoints[this.faceC[index]]) / 3,
				height + (this.yPoints[this.faceA[index]] + this.yPoints[this.faceB[index]] + this.yPoints[this.faceC[index]]) / 3,
				y + (this.zPoints[this.faceA[index]] + this.zPoints[this.faceB[index]] + this.zPoints[this.faceC[index]]) / 3
		);
		return Game.isPointOnScreen(localPoint) ? localPoint : null;
	}

	public Point getCenterPoint() {
		if (numFaces < 1) {
			return new Point(-1, -1);
		}
		update();

		int totalXAverage = 0;
		int totalYAverage = 0;
		int totalHeightAverage = 0;
		int index = 0;

		final int x = getX();
		final int y = getY();
		final int plane = getPlane();
		final int height = Game.tileHeight(x, y, plane);

		while (index < numFaces) {
			totalXAverage += (xPoints[faceA[index]] + xPoints[faceB[index]] + xPoints[faceC[index]]) / 3;
			totalYAverage += (zPoints[faceA[index]] + zPoints[faceB[index]] + zPoints[faceC[index]]) / 3;
			totalHeightAverage += (yPoints[faceA[index]] + yPoints[faceB[index]] + yPoints[faceC[index]]) / 3;
			index++;
		}

		final Point averagePoint = Game.worldToScreen(
				x + totalXAverage / numFaces,
				height + totalHeightAverage / numFaces,
				y + totalYAverage / numFaces
		);

		if (Game.isPointOnScreen(averagePoint)) {
			return averagePoint;
		}
		return new Point(-1, -1);
	}

	public Point getNextPoint() {
		update();
		final int mark = Random.nextInt(0, numFaces);
		Point point = firstOnScreenCentroid(mark, numFaces);
		return point != null ? point : (point = firstOnScreenCentroid(0, mark)) != null ? point : new Point(-1, -1);
	}

	public Polygon[] getTriangles() {
		final int[][] points = projectVertices();
		final ArrayList<Polygon> polygons = new ArrayList<>(numFaces);
		for (int index = 0; index < numFaces; index++) {
			final int index1 = faceA[index];
			final int index2 = faceB[index];
			final int index3 = faceC[index];

			final int xPoints[] = new int[3];
			final int yPoints[] = new int[3];

			xPoints[0] = points[index1][0];
			yPoints[0] = points[index1][1];
			xPoints[1] = points[index2][0];
			yPoints[1] = points[index2][1];
			xPoints[2] = points[index3][0];
			yPoints[2] = points[index3][1];

			if (points[index1][2] + points[index2][2] + points[index3][2] == 3) {
				polygons.add(new Polygon(xPoints, yPoints, 3));
			}
		}
		return polygons.toArray(new Polygon[polygons.size()]);
	}

	public boolean contains(final Point point) {
		final int x = point.x, y = point.y;
		final int[][] points = projectVertices();
		int index = 0;
		while (index < numFaces) {
			final int index1 = faceA[index];
			final int index2 = faceB[index];
			final int index3 = faceC[index];
			if (points[index1][2] + points[index2][2] + points[index3][2] == 3 &&
					barycentric(x, y, points[index1][0], points[index1][1], points[index2][0], points[index2][1], points[index3][0], points[index3][1])) {
				return true;
			}
			++index;
		}
		return false;
	}

	public void drawWireFrame(final Graphics render) {
		final int[][] screen = projectVertices();

		for (int index = 0; index < numFaces; index++) {
			int index1 = faceA[index];
			int index2 = faceB[index];
			int index3 = faceC[index];

			int point1X = screen[index1][0];
			int point1Y = screen[index1][1];
			int point2X = screen[index2][0];
			int point2Y = screen[index2][1];
			int point3X = screen[index3][0];
			int point3Y = screen[index3][1];

			if (screen[index1][2] + screen[index2][2] + screen[index3][2] == 3) {
				render.drawLine(point1X, point1Y, point2X, point2Y);
				render.drawLine(point2X, point2Y, point3X, point3Y);
				render.drawLine(point3X, point3Y, point1X, point1Y);
			}
		}
	}

	private int firstOnScreenIndex(final int pos, final int length) {
		final int x = getX();
		final int y = getY();
		final int plane = getPlane();
		final int h = Game.tileHeight(x, y, plane);
		int index = pos;
		final boolean fixed = Game.isFixed();
		final Component c = Widgets.get(ActionBar.WIDGET, ActionBar.COMPONENT_BAR);
		final Rectangle r = c != null && c.isVisible() ? c.getBoundingRect() : null;
		while (index < length) {
			final Point point = Game.worldToScreen(
					x + (this.xPoints[this.faceA[index]] + this.xPoints[this.faceB[index]] + this.xPoints[this.faceC[index]]) / 3,
					h + (this.yPoints[this.faceA[index]] + this.yPoints[this.faceB[index]] + this.yPoints[this.faceC[index]]) / 3,
					y + (this.zPoints[this.faceA[index]] + this.zPoints[this.faceB[index]] + this.zPoints[this.faceC[index]]) / 3
			);
			if ((r == null || !r.contains(point)) &&
					fixed ? (point.x >= 4 && point.y >= 4 && point.x < 516 && point.y < 388) :
					(point.x != -1 && point.y != -1)) return index;
			++index;
		}
		return -1;
	}

	private Point firstOnScreenCentroid(final int pos, final int length) {
		final int index = firstOnScreenIndex(pos, length);
		return index != -1 ? getCentroid(index) : null;
	}

	private boolean barycentric(int x, int y, int aX, int aY, int bX, int bY, int cX, int cY) {
		final int v00 = cX - aX, v01 = cY - aY;
		final int v10 = bX - aX, v11 = bY - aY;
		final int v20 = x - aX, v21 = y - aY;
		final int d00 = v00 * v00 + v01 * v01, d01 = v00 * v10 + v01 * v11, d02 = v00 * v20 + v01 * v21;
		final int d11 = v10 * v10 + v11 * v11, d12 = v10 * v20 + v11 * v21;
		float denom = 1.0f / (d00 * d11 - d01 * d01);
		float u = (d11 * d02 - d01 * d12) * denom;
		float v = (d00 * d12 - d01 * d02) * denom;
		return u >= 0 && v >= 0 && u + v < 1;
	}

	private int[][] projectVertices() {
		final ClientFactory clientFactory = ClientFactory.getFactory();
		final Game.Viewport viewport = clientFactory.getViewport();
		final Game.Toolkit toolkit = clientFactory.getToolkit();

		update();
		final int locX = getX();
		final int locY = getY();
		final int plane = getPlane();
		final int height = Game.tileHeight(locX, locY, plane);

		final int[][] screen = new int[numVertices][3];
		for (int index = 0; index < numVertices; index++) {
			final int x = xPoints[index] + locX;
			final int y = yPoints[index] + height;
			final int z = zPoints[index] + locY;

			final float _z = (viewport.zOff + (viewport.zX * x + viewport.zY * y + viewport.zZ * z));
			final float _x = (viewport.xOff + (viewport.xX * x + viewport.xY * y + viewport.xZ * z));
			final float _y = (viewport.yOff + (viewport.yX * x + viewport.yY * y + viewport.yZ * z));

			if (_x >= -_z && _x <= _z && _y >= -_z && _y <= _z) {
				screen[index][0] = Math.round(toolkit.absoluteX + (toolkit.xMultiplier * _x) / _z);
				screen[index][1] = Math.round(toolkit.absoluteY + (toolkit.yMultiplier * _y) / _z);
				screen[index][2] = 1;
			} else {
				screen[index][0] = -1;
				screen[index][1] = -1;
				screen[index][2] = 0;
			}
		}
		return screen;
	}

	@Override
	public boolean equals(final Object o) {
		if (o == null || !(o instanceof Model)) return false;
		final Model model = (Model) o;
		return Arrays.equals(faceA, model.faceA) &&
				Arrays.equals(xPoints, model.xPoints) && Arrays.equals(yPoints, model.yPoints) && Arrays.equals(zPoints, model.zPoints);
	}

	@Override
	public String toString() {
		return "[faces=" + numFaces + "vertices=" + numVertices + "] " + Arrays.toString(faceA);
	}
}
