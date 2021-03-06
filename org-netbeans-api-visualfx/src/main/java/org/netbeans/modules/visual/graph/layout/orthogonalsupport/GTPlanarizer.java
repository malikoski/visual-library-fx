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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Random;

import org.netbeans.modules.visual.graph.layout.orthogonalsupport.DualGraph.FaceEdge;
import org.netbeans.modules.visual.graph.layout.orthogonalsupport.DualGraph.FaceVertex;
import org.netbeans.modules.visual.graph.layout.orthogonalsupport.Face.Dart;
import org.netbeans.modules.visual.graph.layout.orthogonalsupport.MGraph.DummyEdge;
import org.netbeans.modules.visual.graph.layout.orthogonalsupport.MGraph.DummyVertex;
import org.netbeans.modules.visual.graph.layout.orthogonalsupport.MGraph.Edge;
import org.netbeans.modules.visual.graph.layout.orthogonalsupport.MGraph.Vertex;

/**
 *
 */
public class GTPlanarizer {

	private final int DEFAULT_ITERATIONS = 1;
	private int maxIterations;
	private boolean isRandom;
	private Random random;

	/** Creates a new instance of GTPlanarizer */
	public GTPlanarizer() {
		random = new Random();
		isRandom = false;
		maxIterations = DEFAULT_ITERATIONS;
	}

	/**
	 * 
	 * @param graph
	 * @return
	 */
	public Collection<EmbeddedPlanarGraph> planarize(MGraph graph) {
		Collection<EmbeddedPlanarGraph> epgs = createPlanarSubgraphs(graph);

		return epgs;
	}

	/**
	 * Goldschmidt/Takvorian Planarization Algorithm
	 * 
	 * @param graph
	 * @return
	 */
	private Collection<EmbeddedPlanarGraph> createPlanarSubgraphs(MGraph graph) {
		Collection<Edge> LMax = null;
		Collection<Edge> RMax = null;
		Collection<Edge> BMax = null;
		ArrayList<Vertex> orderingMax = null;

		for (int i = 0; i < maxIterations; i++) {

			ArrayList<Edge> L = new ArrayList<Edge>();
			ArrayList<Edge> R = new ArrayList<Edge>();
			ArrayList<Edge> B = new ArrayList<Edge>();

			ArrayList<Vertex> ordering = computeOrdering(graph.getVertices());

			computeLRB(L, R, B, ordering);

			if ((LMax == null) || (L.size() + R.size() > LMax.size() + RMax.size())) {

				LMax = L;
				RMax = R;
				BMax = B;
				orderingMax = ordering;
			}

		}

		// Restore ordering specified by orderingMax
		int order = 0;
		for (Vertex v : orderingMax) {
			v.setNumber(order++);
		}

		Collection<Face> leftFaces = new LinkedHashSet<Face>();
		Collection<Face> rightFaces = new LinkedHashSet<Face>();

		Collection<ArrayList<Face>> faceLists = createAllFaces(LMax, RMax, orderingMax, leftFaces, rightFaces);

		Collection<EmbeddedPlanarGraph> epgs = new ArrayList<EmbeddedPlanarGraph>();

		for (ArrayList<Face> faceList : faceLists) {
			EmbeddedPlanarGraph epg = EmbeddedPlanarGraph.createGraph(graph);
			epgs.add(epg);
			epg.addFaces(faceList);
		}

		// We use the right faces for inserting the remaining edges
		// because there should be less of them.
		insertRemainingEdges(epgs, BMax, rightFaces, false, leftFaces, new HashSet<Edge>(LMax));

		return epgs;
	}

	/**
	 * 
	 * @param vertices
	 * @return
	 */
	private ArrayList<Vertex> computeOrdering(Collection<Vertex> vertices) {
		ArrayList<Vertex> ordering = new ArrayList<Vertex>();
		ArrayList<Vertex> vertexArray = new ArrayList<Vertex>(vertices);
		int order = 0;

		// determine the lowest degree of any vertex. Creates a collection of
		// vertices with this degree. Lastly, picks a random vertex with the
		// lowest degree.
		Vertex v = getMinimumDegreeVertex(vertexArray);

		// appending v to the oredering list and setting v's "order" to order
		assignOrdering(ordering, v, order++);
		// now that it is added to the ordered array, remove it from the default
		// list
		vertexArray.remove(v);

		ArrayList<Vertex> candidates = new ArrayList<Vertex>();

		while (!vertexArray.isEmpty()) {
			Collection<Vertex> neighbors = v.getNeighbors();

			for (Vertex nv : neighbors) {
				if (vertexArray.contains(nv)) {
					candidates.add(nv);
				}
			}

			v = getMinimumDegreeVertex(candidates);

			if (v == null) {
				v = vertexArray.get(0);
			}

			assignOrdering(ordering, v, order++);
			vertexArray.remove(v);
			candidates.clear();
		}

		return ordering;
	}

	/**
	 * 
	 * @param vertices
	 * @return
	 */
	private Vertex getMinimumDegreeVertex(Collection<Vertex> vertices) {
		if (vertices.size() == 0) {
			return null;
		}

		ArrayList<Vertex> candidates = new ArrayList<Vertex>();
		int minDegree = getMinimumDegree(vertices);

		for (Vertex v : vertices) {
			if (v.getDegree() == minDegree) {
				candidates.add(v);
			}
		}

		return pickVertex(candidates);
	}

	/**
	 * 
	 * @param vertices
	 * @return
	 */
	private int getMinimumDegree(Collection<Vertex> vertices) {
		int minDegree = Integer.MAX_VALUE;

		for (Vertex v : vertices) {
			int degree = v.getDegree();
			if (degree < minDegree) {
				minDegree = degree;
			}
		}

		return minDegree;
	}

	/**
	 * 
	 * @param candidates
	 * @return
	 */
	private Vertex pickVertex(ArrayList<Vertex> candidates) {
		int index = 0;

		if (isRandom()) {
			index = random.nextInt(candidates.size());
		}

		return candidates.get(index);
	}

	/**
	 * 
	 * @param L
	 * @param R
	 * @param B
	 * @param ordering
	 */
	private void computeLRB(ArrayList<Edge> L, ArrayList<Edge> R, ArrayList<Edge> B, ArrayList<Vertex> ordering) {
		ArrayList<Edge> edges = assignEdgeWeights(ordering);

		int size = edges.size();
		for (int i = 0; i < size; i++) {
			Edge ie = edges.get(i);

			if (R.contains(ie)) {
				continue;
			}

			int ievo = ie.getV().getNumber();
			int iewo = ie.getW().getNumber();
			if (ievo > iewo) {
				int tmp = ievo;
				ievo = iewo;
				iewo = tmp;
			}

			L.add(ie);

			for (int j = i + 1; j < size; j++) {
				Edge je = edges.get(j);

				if (R.contains(je)) {
					continue;
				}

				int jevo = je.getV().getNumber();
				int jewo = je.getW().getNumber();
				if (jevo > jewo) {
					int tmp = jevo;
					jevo = jewo;
					jewo = tmp;
				}

				if ((ievo < jevo && jevo < iewo && iewo < jewo) || (jevo < ievo && ievo < jewo && jewo < iewo)) {
					R.add(je);
				}
			}
		}

		size = R.size();
		for (int i = 0; i < size; i++) {
			Edge ie = R.get(i);

			if (B.contains(ie)) {
				continue;
			}

			int ievo = ie.getV().getNumber();
			int iewo = ie.getW().getNumber();
			if (ievo > iewo) {
				int tmp = ievo;
				ievo = iewo;
				iewo = tmp;
			}

			for (int j = i + 1; j < size; j++) {
				Edge je = R.get(j);

				if (B.contains(je)) {
					continue;
				}

				int jevo = je.getV().getNumber();
				int jewo = je.getW().getNumber();
				if (jevo > jewo) {
					int tmp = jevo;
					jevo = jewo;
					jewo = tmp;
				}

				if ((ievo < jevo && jevo < iewo && iewo < jewo) || (jevo < ievo && ievo < jewo && jewo < iewo)) {
					B.add(je);
				}
			}
		}
		R.removeAll(B);
	}

	/**
	 * 
	 * @param vertices
	 * @return
	 */
	private ArrayList<Edge> assignEdgeWeights(Collection<Vertex> vertices) {
		LinkedHashSet<Edge> edges = new LinkedHashSet<Edge>();

		// First create all the edges
		for (Vertex v : vertices) {
			Collection<Edge> localEdges = v.getEdges();
			for (Edge e : localEdges) {
				edges.add(e);
			}
		}

		if (!isRandom) {
			return new ArrayList<Edge>(edges);
		} else {

			// Randomly assign a weight to each edge
			int size = edges.size();
			for (Edge e : edges) {
				e.setWeight(0); // random.nextInt(size));

			}

			// Sort the edges by weight
			ArrayList<Edge> sortedEdges = new ArrayList<Edge>(edges);

			for (int i = 0; i < size - 2; i++) {
				Edge ie = sortedEdges.get(i);
				for (int j = i + 1; j < size - 1; j++) {
					Edge je = sortedEdges.get(j);
					if (je.getWeight() < ie.getWeight()) {
						sortedEdges.set(i, je);
						sortedEdges.set(j, ie);
						ie = je;
					}
				}
			}

			return sortedEdges;
		}
	}

	/**
	 * 
	 * @param ordering
	 * @param v
	 * @param order
	 */
	private void assignOrdering(ArrayList<Vertex> ordering, Vertex v, int order) {
		// assume vertices are added in order
		ordering.add(v);
		v.setNumber(order);
	}

	/**
	 * 
	 * @param L
	 * @param R
	 * @param ordering
	 * @param leftFaces
	 * @param rightFaces
	 * @return
	 */
	private Collection<ArrayList<Face>> createAllFaces(Collection<Edge> L, Collection<Edge> R,
			ArrayList<Vertex> ordering, Collection<Face> leftFaces, Collection<Face> rightFaces) {
		ArrayList<Face> faces = new ArrayList<Face>();

		leftFaces.addAll(createFaces(L, null, ordering, false));
		rightFaces.addAll(createFaces(R, L, ordering, true));
		faces.addAll(leftFaces);
		faces.addAll(rightFaces);
		removeFaces(faces);

		Collection<ArrayList<Face>> faceLists = computeFaceLists(faces);
		for (ArrayList<Face> faceList : faceLists) {
			Face outerFace = createOuterFace(faceList, L, R);
			faceList.add(0, outerFace);

			rightFaces.add(outerFace);
			leftFaces.add(outerFace);

		}

		return faceLists;
	}

	/**
	 * TODO: need to optimize
	 * 
	 * @param faces
	 * @return
	 */
	private Collection<ArrayList<Face>> computeFaceLists(ArrayList<Face> faces) {
		Collection<ArrayList<Face>> faceLists = new ArrayList<ArrayList<Face>>();
		faces = new ArrayList<Face>(faces);

		while (!faces.isEmpty()) {
			ArrayList<Face> faceList = new ArrayList<Face>();
			faceLists.add(faceList);
			Face firstFace = faces.remove(0);
			faceList.add(firstFace);

			for (int i = 0; i < faceList.size(); i++) {
				Collection<Face> connectedFaces = new ArrayList<Face>();
				Face iface = faceList.get(i);

				for (int j = 0; j < faces.size(); j++) {
					Face jface = faces.get(j);

					if (iface.connects(jface)) {
						connectedFaces.add(jface);
					}
				}

				faces.removeAll(connectedFaces);
				faceList.addAll(connectedFaces);
			}
		}

		return faceLists;
	}

	/**
	 * 
	 * @param faces
	 * @param L
	 * @param R
	 * @return
	 */
	private Face createOuterFace(ArrayList<Face> faces, Collection<Edge> L, Collection<Edge> R) {
		HashSet<Edge> sharedEdges = new HashSet<Edge>();
		HashSet<Dart> outerDarts = new LinkedHashSet<Dart>();
		HashSet<Edge> outerEdges = new HashSet<Edge>();
		ArrayList<Dart> reverseDarts = new ArrayList<Dart>();
		HashSet<Edge> reverseEdges = new HashSet<Edge>();
		HashSet<Edge> leftEdges = new HashSet<Edge>(L);

		int size = faces.size();
		Dart firstDart = null;

		for (int i = 0; i < size; i++) {
			Face f = faces.get(i);

			for (Dart d : f.getDarts()) {

				Edge e = d.getEdge();
				boolean isSharedEdge = false;

				if (sharedEdges.contains(e)) {
					continue;
				}

				for (int j = i + 1; j < size; j++) {
					Face nf = faces.get(j);

					if (nf.containsEdge(e)) {
						sharedEdges.add(e);
						isSharedEdge = true;
						break;
					}
				}

				if (!isSharedEdge) {
					if (firstDart == null) {
						firstDart = d;
						outerEdges.add(e);
					} else {
						if (!outerEdges.contains(e)) {
							outerDarts.add(d);
							outerEdges.add(e);
						} else {
							reverseDarts.add(d);
							reverseEdges.add(d.getEdge());
						}
					}
				}
			}
		}

		outerDarts.addAll(reverseDarts);

		Face outerFace = new Face();
		outerFace.setOuterFace(true);
		Vertex v = firstDart.getV();
		outerDarts.remove(firstDart);
		outerFace.addEdge(firstDart.getEdge());

		ArrayList<Dart> candidateDarts = new ArrayList<Dart>();

		while (!outerDarts.isEmpty()) {
			candidateDarts.clear();
			int vNumber = v.getNumber();

			for (Dart d : outerDarts) {
				if (d.contains(v)) {
					candidateDarts.add(d);
				}
			}

			Dart nextDart = null;
			if (candidateDarts.size() > 1) {
				for (Dart cd : candidateDarts) {
					if (reverseEdges.contains(cd.getEdge())) {
						reverseEdges.remove(cd.getEdge());
						nextDart = cd;
						break;
					}
				}

				if (nextDart == null) {
					for (Dart cd : candidateDarts) {
						if (R.contains(cd.getEdge())) {
							nextDart = cd;
							break;
						}
					}
				}

				if (nextDart == null) {
					for (Dart cd : candidateDarts) {
						if (nextDart == null) {
							nextDart = cd;
						} else {
							Vertex cw = null;
							if (cd.getW().getNumber() > cd.getV().getNumber()) {
								cw = cd.getW();
							} else {
								cw = cd.getV();
							}

							Vertex nw = null;
							if (nextDart.getW().getNumber() > nextDart.getV().getNumber()) {
								nw = nextDart.getW();
							} else {
								nw = nextDart.getV();
							}

							int cNumber = cw.getNumber();
							int nNumber = nw.getNumber();

							if (cNumber > vNumber && cNumber < nNumber) {
								nextDart = cd;
							}
						}
					}
				}
			} else if (candidateDarts.size() > 0) {
				nextDart = candidateDarts.get(0);
			} else {
				nextDart = null;
			}

			if (nextDart != null) {
				outerDarts.remove(nextDart);
				outerFace.addEdge(nextDart.getEdge());
				v = nextDart.getOppositeVertex(v);
			}
		}

		outerFace.setOuterFace(true);
		outerFace.createDarts();

		return outerFace;
	}

	/**
	 * 
	 * @param subgraph
	 * @param alternateSubgraph
	 * @param ordering
	 * @param reverseDirection
	 * @return
	 */
	private Collection<Face> createFaces(Collection<Edge> subgraph, Collection<Edge> alternateSubgraph,
			ArrayList<Vertex> ordering, boolean reverseDirection) {
		Collection<Face> faces = new ArrayList<Face>(); // new HashSet<Face>();

		for (Vertex v : ordering) {
			Collection<Edge> connEdges = getIncidentEdges(v, subgraph);

			while (!connEdges.isEmpty()) {
				int maxOrder = v.getNumber();
				Edge maxEdge = null;

				for (Edge e : connEdges) {
					Vertex w = e.getOppositeVertex(v);
					int order = w.getNumber();
					if (order > maxOrder) {
						maxOrder = order;
						maxEdge = e;
					}
				}

				if (maxEdge == null) {
					break;
				}

				connEdges.remove(maxEdge);

				Collection<Edge> shortestPath = computeShortedReturnPath(v, maxEdge.getOppositeVertex(v), maxEdge,
						subgraph, alternateSubgraph);

				if (!shortestPath.isEmpty()) {
					Face face = new Face();
					face.addEdge(maxEdge);
					face.addEdges(shortestPath);

					boolean invalidFace = false;
					if (face.getDegree() == 2) {
						for (Face f : faces) {
							if (face.borders(f)) {
								invalidFace = true;
								break;
							}
						}
					}
					if (!invalidFace) {
						if (reverseDirection) {
							face.reverseDirection();
						}
						face.createDarts();
						faces.add(face);
					}
				}
			}
		}

		return faces;
	}

	/**
	 * 
	 * @param sourceVertex
	 * @param currentVertex
	 * @param currentEdge
	 * @param edges
	 * @param alternateEdges
	 * @return
	 */
	private Collection<Edge> computeShortedReturnPath(Vertex sourceVertex, Vertex currentVertex, Edge currentEdge,
			Collection<Edge> edges, Collection<Edge> alternateEdges) {

		Collection<Edge> result = new ArrayList<Edge>();
		Collection<Edge> connEdges = getIncidentEdges(currentVertex, edges);
		connEdges.remove(currentEdge);

		int sourceOrder = sourceVertex.getNumber();
		int currentOrder = currentVertex.getNumber();
		Edge edge = null;
		int minOrder = currentOrder;
		boolean lowerOrderFound = false;

		for (Edge e : connEdges) {
			Vertex w = e.getOppositeVertex(currentVertex);
			int order = w.getNumber();

			if ((order < minOrder) && (order >= sourceOrder)) {
				minOrder = order;
				edge = e;
			}

			if (order < currentOrder) {
				lowerOrderFound = true;
			}
		}

		if (edge == null && alternateEdges != null) {
			connEdges = getIncidentEdges(currentVertex, alternateEdges);
			int maxOrder = sourceOrder;

			for (Edge e : connEdges) {
				Vertex w = e.getOppositeVertex(currentVertex);
				int order = w.getNumber();

				if ((order >= maxOrder) && (order < currentOrder)) {
					maxOrder = order;
					edge = e;
				}

				if (order < currentOrder) {
					lowerOrderFound = true;
				}
			}
		}

		if (edge == null && !lowerOrderFound) {
			edge = currentEdge;
		}

		if (edge != null) {
			if (edge.contains(sourceVertex)) {
				result.add(edge);
			} else {
				Collection<Edge> shortestPath = null;
				Vertex oppositeVertex = edge.getOppositeVertex(currentVertex);
				if (!oppositeVertex.equals(currentVertex) || !edge.equals(currentEdge)) {
					shortestPath = computeShortedReturnPath(sourceVertex, oppositeVertex, edge, edges, alternateEdges);
				}

				if (!shortestPath.isEmpty()) {
					result.add(edge);
					result.addAll(shortestPath);
				}
			}
		}

		return result;
	}

	/**
	 * 
	 * @param faces
	 */
	private void removeFaces(Collection<Face> faces) {
		Collection<Face> facesToRemove = new ArrayList<Face>();

		for (Face f : faces) {
			if (f.isOuterFace()) {
				continue;
			}

			if (f.getDegree() == 2) {
				for (Face nf : faces) {
					if (nf.isOuterFace()) {
						continue;
					}

					if (f != nf && f.borders(nf)) {
						facesToRemove.add(f);
					}
				}
			}
		}
		faces.removeAll(facesToRemove);
	}

	/**
	 * TODO: the connected edges should be part of the data structure so we
	 * don't have to recompute them each time.
	 * 
	 * @param v
	 * @param edges
	 * @return
	 */
	private Collection<Edge> getIncidentEdges(Vertex v, Collection<Edge> edges) {
		Collection<Edge> result = new ArrayList<Edge>(v.getEdges());
		result.retainAll(edges);

		return result;
	}

	/**
	 * 
	 * @param epgs
	 * @param edgesToInsert
	 * @param faces
	 * @param isLeftFaces
	 * @param facesToIgnore
	 * @param edgesToIgnore
	 */
	private void insertRemainingEdges(Collection<EmbeddedPlanarGraph> epgs, Collection<Edge> edgesToInsert,
			Collection<Face> faces, boolean isLeftFaces, Collection<Face> facesToIgnore,
			Collection<Edge> edgesToIgnore) {

		if (edgesToInsert.isEmpty()) {
			return;
		}

		for (EmbeddedPlanarGraph epg : epgs) {
			DualGraph dualGraph = DualGraph.createGraph(epg, facesToIgnore, edgesToIgnore);

			for (Edge edge : edgesToInsert) {
				insertEdge(dualGraph, edge, faces, isLeftFaces);
			}

		}
	}

	/**
	 * 
	 * @param graph
	 * @param edgeToInsert
	 * @param faces
	 * @param isLeftFaces
	 */
	private void insertEdge(DualGraph graph, Edge edgeToInsert, Collection<Face> faces, boolean isLeftFaces) {

		EmbeddedPlanarGraph epg = graph.getOriginalGraph();
		MGraph originalGraph = epg.getOriginalGraph();

		Vertex v = edgeToInsert.getV();
		Vertex w = edgeToInsert.getW();
		boolean reverse = false;

		if (v.getNumber() < w.getNumber()) {
			reverse = true;
		}

		ArrayList<FaceEdge> shortestPath = computeShortestPathInDualGraph(graph, v, w, faces);

		// Insert dummy vertices and edges and update the dualGraph

		FaceEdge fe = null;
		FaceVertex fv = null;
		Vertex currentVertex = null;
		Vertex prevVertex = v;
		Edge newEdge = null;
		Edge prevEdge = null;
		ArrayList<Edge> newFaceEdges = null;
		int length = shortestPath.size();

		for (int i = 0; i <= length; i++) {
			if (i == length) {
				currentVertex = w;
			} else {
				fe = shortestPath.get(i);

				if (i == 0) {
					fv = fe.getVertex(v);
				}

				if (newFaceEdges != null) {
					fv.getFace().replaceEdge(prevEdge, newFaceEdges);
				}

				currentVertex = originalGraph.insertDummyVertex(fe.getEdge(), DummyVertex.Type.CROSSING);

				((DummyVertex) currentVertex).setNumber(Integer.MAX_VALUE);
				newFaceEdges = new ArrayList<Edge>(currentVertex.getEdges());

			}

			newEdge = originalGraph.addDummyEdge(prevVertex, currentVertex);
			((DummyEdge) newEdge).setOriginalEdge(edgeToInsert);

			Vertex startingVertex = null;

			if (isLeftFaces) {
				if (reverse) {
					startingVertex = currentVertex;
				} else {
					startingVertex = prevVertex;
				}
			} else {
				if (reverse) {
					startingVertex = prevVertex;
				} else {
					startingVertex = currentVertex;
				}
			}

			subdivide(graph, fv, fe, newFaceEdges, newEdge, startingVertex, faces);

			prevVertex = currentVertex;
			prevEdge = fe.getEdge();

			if (i < length) {
				fv = fe.getOppositeVertex(fv);
			}
		}

		removeFaces(epg.getFaces());
		graph.updateFaces();
		graph.updateEdges();
	}

	/**
	 * 
	 * @param v
	 * @param graph
	 * @param rightFaces
	 * @return
	 */
	private Collection<FaceVertex> getIncidentFaceVertices(Vertex v, DualGraph graph, Collection<Face> rightFaces) {

		HashSet<FaceVertex> result = new LinkedHashSet<FaceVertex>();
		FaceVertex outerFace = null;

		Collection<Edge> edges = v.getEdges();
		for (Edge e : edges) {
			Collection<FaceVertex> faceVertices = graph.getVerticesBorderingEdge(e);

			for (FaceVertex fv : faceVertices) {
				Face f = fv.getFace();

				if (rightFaces.contains(f)) {
					if (f.isOuterFace()) {
						outerFace = fv;
						continue;
					}

					result.add(fv);
				}
			}
		}

		if (result.isEmpty()) {
			result.add(outerFace);
		}

		return result;
	}

	/**
	 * TODO: Need to optimize this algorithm. We only compute the shortest path
	 * traversing the right faces since there should be less of them.
	 * 
	 * @param graph
	 * @param v
	 * @param w
	 * @param rightFaces
	 * @return
	 */
	private ArrayList<FaceEdge> computeShortestPathInDualGraph(DualGraph graph, Vertex v, Vertex w,
			Collection<Face> rightFaces) {
		EmbeddedPlanarGraph epg = graph.getOriginalGraph();
		Collection<FaceVertex> vFaceVertices = getIncidentFaceVertices(v, graph, rightFaces);
		Collection<FaceVertex> wFaceVertices = getIncidentFaceVertices(w, graph, rightFaces);

		ArrayList<FaceEdge> shortestPath = null;
		int shortestPathLength = Integer.MAX_VALUE;

		for (FaceVertex vfv : vFaceVertices) {
			for (FaceVertex wfv : wFaceVertices) {
				if (vfv == wfv) {
					continue;
				}

				ArrayList<FaceEdge> path = computeShortestPathInDualGraph(vfv, wfv, new HashSet<FaceVertex>(),
						shortestPathLength, rightFaces);

				if (path == null) {
					continue;
				}

				if (shortestPath == null || path.size() < shortestPath.size()) {
					shortestPath = path;
					shortestPathLength = shortestPath.size();
					// Can't get any shorter than 1
					if (shortestPath.size() == 1) {
						return shortestPath;
					}
				}
			}
		}

		return shortestPath;
	}

	/**
	 * 
	 * @param sourceVertex
	 * @param destVertex
	 * @param visitedVertices
	 * @param minPathLength
	 * @param faces
	 * @return
	 */
	private ArrayList<FaceEdge> computeShortestPathInDualGraph(FaceVertex sourceVertex, FaceVertex destVertex,
			Collection<FaceVertex> visitedVertices, int minPathLength, Collection<Face> faces) {

		ArrayList<FaceEdge> shortestPath = null;

		visitedVertices.add(sourceVertex);

		for (FaceEdge e : sourceVertex.getEdges()) {

			if (e.contains(destVertex)) {
				shortestPath = new ArrayList<FaceEdge>();
				shortestPath.add(e);
				break;
			} else {
				FaceVertex oppositeVertex = e.getOppositeVertex(sourceVertex);

				if (!faces.contains(oppositeVertex.getFace())) {
					continue;
				}

				if (visitedVertices.contains(oppositeVertex)) {
					continue;
				}

				if (visitedVertices.size() - 1 > minPathLength) {
					continue;
				}

				Collection<FaceEdge> path = computeShortestPathInDualGraph(oppositeVertex, destVertex, visitedVertices,
						minPathLength, faces);

				if (path == null) {
					continue;
				}

				if (shortestPath == null || path.size() < shortestPath.size() - 1) {
					shortestPath = new ArrayList<FaceEdge>();
					shortestPath.add(e);
					shortestPath.addAll(path);
					minPathLength = shortestPath.size();
				}
			}
		}

		visitedVertices.remove(sourceVertex);

		return shortestPath;
	}

	/**
	 * 
	 * @param graph
	 * @param fv
	 * @param fe
	 * @param newFaceEdges
	 * @param dividingEdge
	 * @param startingVertex
	 * @param faces
	 */
	public void subdivide(DualGraph graph, FaceVertex fv, FaceEdge fe, Collection<Edge> newFaceEdges, Edge dividingEdge,
			Vertex startingVertex, Collection<Face> faces) {

		Face face = fv.getFace();
		face.replaceEdge(fe.getEdge(), newFaceEdges);

		Collection<Dart> removedDarts = face.replaceDarts(dividingEdge, startingVertex);

		Face newFace = new Face();
		Dart firstDart = null;

		for (Dart d : removedDarts) {
			if (firstDart == null) {
				firstDart = d;
			}
			newFace.addEdge(d.getEdge());
		}
		newFace.addEdge(dividingEdge);
		newFace.createDarts(firstDart.getV());

		EmbeddedPlanarGraph epg = graph.getOriginalGraph();
		epg.addFace(newFace);
		faces.add(newFace);

		graph.updateFaces();
	}

	/**
	 * 
	 * @param flag
	 */
	public void setRandom(boolean flag) {
		isRandom = flag;
	}

	/**
	 * 
	 * @return
	 */
	public boolean isRandom() {
		return isRandom;
	}

	/**
	 * 
	 * @param iterations
	 */
	public void setMaxIterations(int iterations) {
		this.maxIterations = iterations;
	}

	/**
	 * 
	 * @return
	 */
	private int getMaxIterations() {
		return maxIterations;
	}
}
