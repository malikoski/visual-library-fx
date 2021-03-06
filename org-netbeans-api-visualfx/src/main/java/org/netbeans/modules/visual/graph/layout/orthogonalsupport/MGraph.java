/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * Oracle and Java are registered trademarks of Oracle and/or its affiliates.
 * Other names may be trademarks of their respective owners.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */
package org.netbeans.modules.visual.graph.layout.orthogonalsupport;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

import org.netbeans.api.visual.graph.GraphScene;
import org.netbeans.api.visual.graph.layout.UniversalGraph;
import org.netbeans.api.visual.widget.Widget;

public class MGraph<N, E> {

	private Collection<N> nodes;
	private Map<N, Vertex> vertexMap;
	private Map<E, Edge> edgeMap;
	private Collection<Vertex> vertices;
	private Collection<Edge> edges;
	private UniversalGraph<N, E> uGraph = null;
	private GraphScene scene = null;

	/**
	 * 
	 * @param uGraph
	 * @param scene
	 */
	protected MGraph(UniversalGraph<N, E> uGraph, GraphScene scene) {
		this.uGraph = uGraph;
		this.scene = scene;
		this.nodes = uGraph.getNodes();

		vertexMap = new HashMap<N, Vertex>();
		edgeMap = new LinkedHashMap<E, Edge>();
		vertices = new ArrayList<Vertex>();

		DummyVertex.resetCounter();
	}

	/**
	 * 
	 * @param uGraph
	 * @param scene
	 * @return
	 */
	public static <N, E> MGraph createGraph(UniversalGraph<N, E> uGraph, GraphScene scene) {
		MGraph<N, E> graph = new MGraph<N, E>(uGraph, scene);
		graph.createGraph();
		return graph;
	}

	/**
	 * 
	 */
	protected void createGraph() {
		for (N node : nodes) {
			// will create a vertex if one does not exist.
			Vertex v = getVertex(node);
			Widget widget = scene.findWidget(node);
			Rectangle bounds = widget.getBounds();

			Dimension size = new Dimension(bounds.width, bounds.height);

			v.setSize(size);

			// out going edges
			Collection<E> nodeEdges = uGraph.findNodeEdges(node, true, false);

			for (E edge : nodeEdges) {

				N destNode = uGraph.getEdgeTarget(edge);
				Vertex nv = getVertex(destNode);

				// Implicitly preserve the direction of the edge
				Edge e = getEdge(edge, v, nv);
				v.addNeighbor(nv);
				v.addEdge(e);
				nv.addEdge(e);
			}

			// incoming edges
			nodeEdges = uGraph.findNodeEdges(node, false, true);
			for (E edge : nodeEdges) {
				N destNode = uGraph.getEdgeSource(edge);
				Vertex nv = getVertex(destNode);

				// Implicitly preserve the direction of the edge.
				Edge e = getEdge(edge, nv, v);
				v.addNeighbor(nv);
				v.addEdge(e);
				nv.addEdge(e);
			}
		}
	}

	/**
	 * 
	 * @return
	 */
	public Collection<Vertex> getVertices() {
		return Collections.unmodifiableCollection(vertices);
	}

	/**
	 * 
	 * @return
	 */
	public Collection<Edge> getEdges() {
		return Collections.unmodifiableCollection(edgeMap.values());
	}

	/**
	 * 
	 * @param node
	 * @return
	 */
	protected Vertex getVertex(N node) {
		Vertex vertex = vertexMap.get(node);

		if (vertex == null) {
			vertex = createVertex(node);
			vertexMap.put(node, vertex);
			vertices.add(vertex);
		}

		return vertex;
	}

	/**
	 * 
	 * @param node
	 * @return
	 */
	protected Vertex createVertex(N node) {
		return new Vertex(node);
	}

	/**
	 * 
	 * @param edgeDE
	 * @param v
	 * @param w
	 * @return
	 */
	protected Edge getEdge(E edgeDE, Vertex v, Vertex w) {
		Edge edge = edgeMap.get(edgeDE);

		if (edge == null) {
			edge = createEdge(v, w, edgeDE);
			edgeMap.put(edgeDE, edge);
		}

		return edge;
	}

	/**
	 * 
	 * @param v
	 * @param w
	 * @param edgeDE
	 * @return
	 */
	protected Edge createEdge(Vertex v, Vertex w, E edgeDE) {
		return new Edge(v, w, edgeDE);
	}

	/**
	 * 
	 * @param edge
	 * @param type
	 * @return
	 */
	public DummyVertex insertDummyVertex(Edge edge, DummyVertex.Type type) {
		Edge originalEdge = edge;

		if (edge instanceof DummyEdge) {
			originalEdge = ((DummyEdge) edge).getOriginalEdge();
		}

		DummyVertex dv = createDummyVertex(originalEdge, type);
		vertices.add(dv);

		Vertex v = edge.getV();
		Vertex w = edge.getW();

		v.removeEdge(edge);
		v.removeNeighbor(w);
		v.addNeighbor(dv);
		dv.addNeighbor(v);
		DummyEdge de = createDummyEdge(v, dv, originalEdge);
		v.addEdge(de);
		dv.addEdge(de);

		w.removeEdge(edge);
		w.removeNeighbor(v);
		w.addNeighbor(dv);
		dv.addNeighbor(w);
		de = createDummyEdge(dv, w, originalEdge);
		w.addEdge(de);
		dv.addEdge(de);

		return dv;
	}

	/**
	 * 
	 * @param originalEdge
	 * @param type
	 * @return
	 */
	protected DummyVertex createDummyVertex(Edge originalEdge, DummyVertex.Type type) {
		return new DummyVertex(originalEdge, type);
	}

	/**
	 * 
	 * @param v
	 * @param w
	 * @return
	 */
	public DummyEdge addDummyEdge(Vertex v, Vertex w) {
		DummyEdge de = createDummyEdge(v, w, null);
		v.addEdge(de);
		w.addEdge(de);
		v.addNeighbor(w);
		w.addNeighbor(v);

		return de;
	}

	/**
	 * 
	 * @param v
	 * @param w
	 * @param originalEdge
	 * @return
	 */
	protected DummyEdge createDummyEdge(Vertex v, Vertex w, Edge originalEdge) {
		return new DummyEdge(v, w, originalEdge);
	}

	/**
	 * 
	 * @param type
	 * @return
	 */
	public DummyVertex addDummyVertex(DummyVertex.Type type) {
		DummyVertex dv = createDummyVertex(null, type);
		vertices.add(dv);

		return dv;
	}

	/**
	 *
	 *
	 */
	public void printGraph() {
		int count = 0;
		for (Vertex<N> v : getVertices()) {
			Logger.log(1, count + ") vertex = " + v + " (" + v.getX() + ", " + v.getY() + ")");
			count++;
			// out going edges
			N node = v.getNodeDesignElement();
			if (node == null) {
				continue;
			} // if the vertex is a dummy, there is no
				// node associated with it.

			Collection<E> nodeEdges = uGraph.findNodeEdges(node, true, false);
			Logger.log(1, "\toutgoing edges:");
			for (E edge : nodeEdges) {
				Logger.log(1, "\t\t" + edge);
			}

			nodeEdges = uGraph.findNodeEdges(node, false, true);
			Logger.log(1, "\tincoming edges:");
			for (E edge : nodeEdges) {
				Logger.log(1, "\t\t" + edge);
			}

			Logger.log(1, "\tneighbors:");
			Collection<Vertex> neighbors = v.getNeighbors();
			for (Vertex nv : neighbors) {
				Logger.log(1, "\t\t" + nv);
			}

		}

		Logger.log(1, "------------------\n------------------");
		count = 0;
		for (Edge e : getEdges()) {
			Logger.log(1, count + ") edge = " + e);
			count++;
		}
	}

	/**
	 * 
	 * @param N
	 */
	public static class Vertex<N> {

		private N node;
		private Collection<Vertex> neighbors;
		private Collection<Edge> edges;
		private int number = -1;
		private Object vertexData;
		private float x;
		private float y;
		private Dimension size = null;

		/**
		 * 
		 * @param node
		 */
		public Vertex(N node) {
			this.node = node;
			neighbors = new LinkedHashSet<Vertex>();
			edges = new LinkedHashSet<Edge>();
		}

		/**
		 * 
		 * @return
		 */
		public Dimension getSize() {
			return size;
		}

		/**
		 * 
		 * @param dim
		 */
		public void setSize(Dimension dim) {
			this.size = dim;
		}

		/**
		 * 
		 * @return
		 */
		public float getX() {
			return x;
		}

		/**
		 * 
		 * @param x
		 */
		public void setX(float x) {
			this.x = x;
		}

		/**
		 * 
		 * @return
		 */
		public float getY() {
			return y;
		}

		/**
		 * 
		 * @param y
		 */
		public void setY(float y) {
			this.y = y;
		}

		/**
		 * 
		 * @param vertex
		 */
		public void addNeighbor(Vertex vertex) {
			neighbors.add(vertex);
		}

		/**
		 * 
		 * @param vertex
		 */
		public void removeNeighbor(Vertex vertex) {
			neighbors.remove(vertex);
		}

		/**
		 * 
		 * @return
		 */
		public Collection<Vertex> getNeighbors() {
			return neighbors;
		}

		/**
		 * 
		 * RESOLVE: need to optimize this.
		 * 
		 * @param neighbor
		 * @return
		 */
		public Edge getEdge(Vertex neighbor) {
			for (Edge e : edges) {
				if (e.contains(neighbor)) {
					return e;
				}
			}

			return null;
		}

		/**
		 * 
		 * @param edge
		 */
		public void addEdge(Edge edge) {
			edges.add(edge);
		}

		/**
		 * 
		 * @param edge
		 */
		public void removeEdge(Edge edge) {
			edges.remove(edge);
		}

		/**
		 * 
		 * @return
		 */
		public Collection<Edge> getEdges() {
			return edges;
		}

		/**
		 * 
		 * @return
		 */
		public N getNodeDesignElement() {
			return node;
		}

		/**
		 * 
		 * @return
		 */
		public int getDegree() {
			return neighbors.size();
		}

		/**
		 * 
		 * @return
		 */
		public int getNumber() {
			return number;
		}

		/**
		 * 
		 * @param number
		 */
		public void setNumber(int number) {
			this.number = number;
		}

		/**
		 * 
		 * @param data
		 */
		public void setVertexData(Object data) {
			this.vertexData = data;
		}

		/**
		 * 
		 * @return
		 */
		public Object getVertexData() {
			return vertexData;
		}

		/**
		 * 
		 * @return
		 */
		public String toString() {
			return "vertex : " + node;// + " number = " + number;

		}
	}

	/**
	 * TODO: Should be in MGraph
	 */
	public static class Edge<E> {

		public enum Direction {

			HORIZONTAL, VERTICAL, UP, DOWN, LEFT, RIGHT
		}

		private Vertex v;
		private Vertex w;
		private E edge;
		private Direction direction;
		private int weight;
		private Object edgeData;

		/**
		 * 
		 * @param v
		 * @param w
		 * @param edge
		 */
		public Edge(Vertex v, Vertex w, E edge) {
			this.v = v;
			this.w = w;
			this.edge = edge;
		}

		/**
		 * 
		 * @return
		 */
		public Vertex getV() {
			return v;
		}

		/**
		 * 
		 * @return
		 */
		public Vertex getW() {
			return w;
		}

		/**
		 * 
		 * @return
		 */
		public int getWeight() {
			return weight;
		}

		/**
		 * 
		 * @param weight
		 */
		public void setWeight(int weight) {
			this.weight = weight;
		}

		/**
		 * 
		 * @return
		 */
		public E getEdgeDesignElement() {
			return edge;
		}

		/**
		 * 
		 * @param data
		 */
		public void setEdgeData(Object data) {
			this.edgeData = data;
		}

		/**
		 * 
		 * @return
		 */
		public Object getEdgeData() {
			return edgeData;
		}

		/**
		 * 
		 * @param vertex
		 * @return
		 */
		public boolean contains(Vertex vertex) {
			return (this.v == vertex || this.w == vertex);
		}

		/**
		 * 
		 * @param edge
		 * @return
		 */
		public boolean shareVertex(Edge edge) {
			return contains(edge.v) || contains(edge.w);
		}

		/**
		 * 
		 * @param vertex
		 * @return
		 */
		public Vertex getOppositeVertex(Vertex vertex) {
			if (v == vertex) {
				return w;
			} else if (w == vertex) {
				return v;
			}

			return null;
		}

		/**
		 * 
		 * @param direction
		 */
		public void setDirection(Direction direction) {
			this.direction = direction;
		}

		/**
		 * 
		 * @return
		 */
		public Direction getDirection() {
			return direction;
		}

		/**
		 * 
		 * @return
		 */
		public String toString() {
			return "edge : " + edge + "\n  v = " + v + "\n  w = " + w;
		}
	}

	/**
	 * 
	 */
	public static class DummyVertex extends Vertex {

		public enum Type {

			CROSSING, HYPEREDGE, BEND, TEMPORARY
		};

		private static int counter = 0;
		private Edge originalEdge;
		private Type type;
		private int index;

		/**
		 * 
		 * @param originalEdge
		 * @param type
		 */
		public DummyVertex(Edge originalEdge, Type type) {
			super(null);
			this.originalEdge = originalEdge;
			this.type = type;
			index = --counter;
		}

		/**
		 * 
		 * @param type
		 */
		public DummyVertex(Type type) {
			this(null, type);
		}

		/**
		 * 
		 */
		public static void resetCounter() {
			counter = 0;
		}

		/**
		 * 
		 * @param originalEdge
		 */
		public void setOriginalEdge(Edge originalEdge) {
			this.originalEdge = originalEdge;
		}

		/**
		 * 
		 * @return
		 */
		public Edge getOriginalEdge() {
			return originalEdge;
		}

		/**
		 * 
		 * @return
		 */
		public Type getType() {
			return type;
		}

		/**
		 * 
		 * @return
		 */
		public String toString() {
			return "dummy vertex " + index;
		}
	}

	/**
	 * 
	 */
	public static class DummyEdge extends Edge {

		private Edge originalEdge;

		/**
		 * 
		 * @param v
		 * @param w
		 * @param originalEdge
		 */
		public DummyEdge(Vertex v, Vertex w, Edge originalEdge) {
			super(v, w, null);
			this.originalEdge = originalEdge;
		}

		/**
		 * 
		 * @param v
		 * @param w
		 */
		public DummyEdge(Vertex v, Vertex w) {
			this(v, w, null);
		}

		/**
		 * 
		 * @param originalEdge
		 */
		public void setOriginalEdge(Edge originalEdge) {
			this.originalEdge = originalEdge;
		}

		/**
		 * 
		 * @return
		 */
		public Edge getOriginalEdge() {
			return originalEdge;
		}

		/**
		 * 
		 * @return
		 */
		public String toString() {
			return "dummy " + super.toString();
		}
	}
}
