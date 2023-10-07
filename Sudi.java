/**
 * Problem Set 5 (The Sudi speech tagger)
 * @author Tayeb Mohammadi
 */


import java.io.*;
import java.util.*;

public class Sudi {
    boolean skip = false;
    // The node within the hidden Markov graph
    public class Node{
        Map<String, Map<String, Integer> >theMap = new HashMap<>(); // Type --> Words --> Frequency
        Map<String, Integer> wordToFreq = new HashMap<>();
        String typeName;
        String word;
        int Score;
        public Node(String word, int Score, String type){
            this.word = word;
            this.Score = Score;
            this.wordToFreq.put(this.word, this.Score);
            this.typeName = type;
            this.theMap.put(this.typeName,wordToFreq);
        }

    }

    Node root = new Node(null, 0, "#"); //starting Node

    /**
     * Training method Creating the Markov Graph
     * @param typeFilename the file with types
     * @param wordsFilename the fiel with words
    */
    public Graph<Node, Integer> Training (String wordsFilename, String typeFilename) throws IOException {

        Graph<Node, Integer> theMapping = new AdjacencyMapGraph<>();  // come back later to implement the comparator
        theMapping.insertVertex(root); // insert the starting node

        BufferedReader readWords = new BufferedReader(new FileReader(wordsFilename)); // is the words in each line I read
        BufferedReader readType = new BufferedReader(new FileReader(typeFilename)); // types in each line I read
        String wordRead =  readWords.readLine(); // Keep track of the word that is read
        String typeRead = readType.readLine(); // Keep track of the Type read

        while((wordRead != null) && typeRead != null) {
            String[] Words = wordRead.split(" "); // get all the words that you read differently
            String[] Types = typeRead.split(" "); // get their respective types
            Node past = root;
            for (int i = 0; i < Types.length; i++) { // create a node for each type of word
                int freq = 1;
                Node newVertex = new Node(Words[i], freq, Types[i]);
                    for (Node vertex : theMapping.vertices()) {  // compare with others and check if node is already there
                        if (Objects.equals(vertex.typeName, newVertex.typeName)) {
                            skip = true;
                            vertex.wordToFreq.put(newVertex.word, newVertex.Score); // what if the node had the same word in its map
                            vertex.theMap.put(vertex.typeName,vertex.wordToFreq );
                            newVertex = vertex;
                        }
                    }

                    if (!skip) {  // if its type name has not been seen yet, insert it
                        theMapping.insertVertex(newVertex);
                        theMapping.insertDirected(past, newVertex, 1);
                    } else {
                        if (!theMapping.hasEdge(past, newVertex)) {
                            theMapping.insertVertex(newVertex);
                            theMapping.insertDirected(past, newVertex, 1);
                        } else {
                            Integer label = theMapping.getLabel(past, newVertex) + 1;
                            theMapping.removeDirected(past, newVertex);
                            theMapping.insertDirected(past, newVertex, label);
                        }
                    }
                    theMapping.insertDirected(past, newVertex, 1);
                    past = newVertex;
                }
                skip = false;
                wordRead = readWords.readLine();
                typeRead = readType.readLine();
            }


        return theMapping; // our final hidden Markov graph
    }

    /**
     * Viterbi method,
     * @param theGraph the trained graph
     * @param Start the starting node
     * @param observations the words
     */

    public static String viterbi(Graph<Node, Integer> theGraph, Node Start, String [] observations) throws IOException {
        String myGuesses = "";
        List<Map<String, Map<String, Integer>>> backTrack = new ArrayList<>();// The BackTrack Map
        ArrayList<Node> currentStates=  new ArrayList<>(); // current states
        Map<Node, Integer> currentScores = new HashMap<>();
        currentStates.add(Start);
        currentScores.put(Start, 0);

            for(int j = 0; j < observations.length; j++)
            {
                Map<String, Map<String, Integer>> anotherMap = new HashMap<>();// how do I fill this one
                ArrayList<Node> nextStates =  new ArrayList<>(); // next states
                Map<Node, Integer> nextScores = new HashMap<>(); // Mapping of the next Maps and next scores

                for(Node currState: currentStates){
                    int nextScore = 0; // next score for each next state
                    for(Node nextState: theGraph.outNeighbors(currState)){ // get the outgoing nodes
                        nextStates.add(nextState); // add the next states
                        if(nextState.wordToFreq.containsKey(observations[j])){ // the mapping
                            nextScore = currentScores.get(currState) + theGraph.getLabel(currState, nextState) + nextState.wordToFreq.get(observations[j]);
                        } else{
                            nextScore = currentScores.get(currState) + theGraph.getLabel(currState, nextState) - 10;
                        }

                        if(!nextScores.containsKey(nextState) || nextScore > nextScores.get(nextState)){
                            nextScores.put(nextState, nextScore);
                            Map<String, Integer> adjacent = new HashMap<>();
                            adjacent.put(currState.typeName, nextScore);
                            anotherMap.put(nextState.typeName, adjacent);
                        }
                    }

                }

                backTrack.add(anotherMap);
                currentStates = nextStates;
                currentScores = nextScores;
            }

        int largest = 0;
        String Observed = "";
        String current = "";
        for(Map.Entry<String, Map<String, Integer>> lastMapOfMapElement: backTrack.get(backTrack.size()-1).entrySet()){
            for(Map.Entry<String, Integer> innerMapOfLast: lastMapOfMapElement.getValue().entrySet()){ // get the largetst for backtracking
                if(innerMapOfLast.getValue() > largest){
                    largest = innerMapOfLast.getValue();
                    Observed = lastMapOfMapElement.getKey();
                    current = innerMapOfLast.getKey();
                }
            }
        }

        myGuesses = myGuesses + Observed;
        int i = backTrack.size() - 2;
        while(!(i < 0)){

            myGuesses = current + "/ " + myGuesses;
            Map<String, Map<String, Integer>> theMap = backTrack.get(i);
            Map<String, Integer> another = theMap.get(current);

            for(Map.Entry<String, Integer> rec: another.entrySet()){
                current = rec.getKey();

            }
            i--;
        }
        System.out.println(myGuesses);

        return myGuesses;
    }

    /**
     * Testing method using file ,
     * @param testSentencePathName the file with types
     * @param trainTagTestFile the file with train tags
     * @param trainSentenceTestFile flile with train sentences
     *
     */
    public static void inputTest(String testSentencePathName, String trainSentenceTestFile, String trainTagTestFile) throws IOException {
        Sudi mySudi = new Sudi();
        Graph<Node, Integer> markovGraph = mySudi.Training(trainSentenceTestFile, trainTagTestFile); /// train the model
        BufferedReader TestRead = new BufferedReader(new FileReader(testSentencePathName)); // test the mdoel
        String read = TestRead.readLine();
        read.toLowerCase();
        while(read != null){
            String [] observation  = read.split(" ");
            viterbi(markovGraph, mySudi.root, observation);
            read = TestRead.readLine();
        }

    }

    //console based test method
    public static void consoleTest(String trainSentenceTestFile, String trainTagTestFile) throws IOException {
        Sudi mySudi = new Sudi();
        Scanner scanner = new Scanner(System.in);
        Graph<Node, Integer> markovGraph = mySudi.Training(trainSentenceTestFile, trainTagTestFile);
        System.out.println("Enter the sentence:  ");
        String inputted = scanner.nextLine().toLowerCase();
        String[] input = inputted.split("");

        viterbi(markovGraph, mySudi.root, input);
    }

    public void hardcodedGraphTest(){
        Sudi mySudi = new Sudi();
        //hard coded graph test
        Graph<Node, Integer> relationships = new AdjacencyMapGraph<Node, Integer>();
        Node node1 = new Node("he", 1, "P");
        Node node2 = new Node("jumped", 1, "AD");
        Node node3 = new Node("the", 1, "DET");
        Node node4 = new Node("dog", 1, "N");
        Node node5 = new Node("your", 1, "PRO");

        relationships.insertVertex(node1);
        relationships.insertVertex(node2);
        relationships.insertVertex(node3);
        relationships.insertVertex(node4);
        relationships.insertVertex(node5);
        relationships.insertDirected(mySudi.root, node1, 6);
        relationships.insertDirected(mySudi.root, node2, 5);
        relationships.insertDirected(mySudi.root, node3, 2);
        relationships.insertDirected(mySudi.root, node4, 1);
        relationships.insertDirected(mySudi.root, node5, 9);

        relationships.insertDirected(node2, node3, 4);
        relationships.insertDirected(node5, node4, 3);
        relationships.insertUndirected(node1, node2, 1);
        relationships.insertDirected(node2, node1, 5);
        relationships.insertDirected(node1,node3 , 7);
        
    }


public static void main(String[] args) throws IOException {
        Sudi mySudi = new Sudi();
        mySudi.hardcodedGraphTest(); // for the hard coded
        inputTest("texts/simple-test-sentences.txt", "texts/simple-train-sentences.txt", "texts/simple-train-tags.txt");
        consoleTest("texts/simple-train-sentences.txt", "texts/simple-train-tags.txt");
}
}
