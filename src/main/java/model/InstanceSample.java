package model;

import lombok.Data;
import org.jgrapht.DirectedGraph;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;

/**
 * @author Wang Li
 * @description
 * @date 9/14/21 9:39 PM
 */
@Data
public class InstanceSample {
    private int[][] travelTimeMatrix;
    private int[][] coordinate;
    private int parcelNum;
    private DirectedGraph<Integer, DefaultWeightedEdge> routingGraph;
    private int[] timeLimit;




}
