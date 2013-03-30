package organism;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.jdom.Element;
import organism.general.Intensity;
import organism.responding.Action;
import organism.responding.Response;
import organism.stimulation.Stimulation;
import organism.stimulation.Stimulation.StimulusStatus;
import organism.stimulation.Stimulus;

import simulator.agent.AbstractAgent;
import simulator.agent.IBehavioralAgent;
import simulator.agent.action.EnvironmentAction;
import simulator.agent.stimuli.EnvironmentStimulus;
import simulator.components.AComponentInfo;
import simulator.components.IXMLComponentInitializer;


@AComponentInfo(
    id = "organism.OrganismComponent",
    version = 1,
    name = "Organism",
    description = "Represents an Organism.",
    type = AComponentInfo.ComponentType.AGENT
 )
public class OrganismComponent extends AbstractAgent implements IXMLComponentInitializer, IBehavioralAgent{
  
  private static final long serialVersionUID = 1L;
  
  
  Organism organism = null;
  
  /**
   * The current instant.
   */
  int currentInstant = 0;

  /**
   * Current stimulations.
   */
  Set<Stimulation> stimulations = new HashSet<Stimulation>();
  
  /**
   * Every possible environment stimulus is associated to some kind of stimulation.
   */
  Map<EnvironmentStimulus, Stimulation> stimulus2Stimulation = new HashMap<EnvironmentStimulus, Stimulation>();
  
  /**
   * The set of actions being currently emitted.
   */
  Set<EnvironmentAction> emitting = new HashSet<EnvironmentAction>();

  public OrganismComponent(){
    // TODO load the organism correctly!!
//    organism = Organism.createTestOrganism();
//    System.out.println("FIX: load organism XML!!!");
  }
  
  
  @Override
  public void initialize(Element domElement) {
    // Creates and initializes the organism
    organism = new Organism();
    organism.loadXML(domElement);
    
    // Setup initial stimulation
    for(EnvironmentStimulus es: this.possibleStimuli()){
      Stimulation st = new Stimulation((Stimulus)es.getInternalComponentRepresentation(), new Intensity(1.0),
          StimulusStatus.ABSENT);
      
      stimulus2Stimulation.put(es, st);
      stimulations.add(st);
    }
    
  }

  
  @Override
  public void step(){
    
    //
    // Run the usual organism iteration operation.
    //
    
    organism.performStimuliProcessing(stimulations, currentInstant);
    organism.performBehaviorSelection(stimulations, currentInstant);
    
    organism.performConflictResolution();
    organism.performResponseEmission(currentInstant);
    organism.performResponseMaintenance();
    organism.performOperantOp(stimulations, currentInstant);
    organism.performOperantFormationOp(stimulations, currentInstant);
    organism.performOperantEliminationOp();
    organism.performDrivesUpdate(stimulations);
    organism.performEmotionUpdate();
    
    // The component is responsible for managing the time given to the organism
    this.currentInstant++;
    
    // Collect responses to figure out what the organism is doing
    Set<Response> responses = organism.getRespondingSubsystem().getCurrentResponses().getActiveResponses();
    updateActionStatus(responses);


  }

  
  private void updateActionStatus(Set<Response> responses){
    // Must clear current actions
    emitting.clear();
    
    // Add only the ones currently being emitted
    for(Response r: responses){
      emitting.add(new EnvironmentAction(r.getAction().getName()));
    }
  }

  @Override
  public ActionStatus getActionStatus(EnvironmentAction action) {
    if(emitting.contains(action)){
      return ActionStatus.EMITTING;
    }
    else{
      return ActionStatus.NOT_EMITTING;
    }
  }

  @Override
  public Collection<EnvironmentAction> possibleActions() {
    Collection<EnvironmentAction> actions = new LinkedList<EnvironmentAction>();

    // Gets all the organism actions available, and convert them to environment actions
    Set<Action> organismActions = organism.getRespondingSubsystem().getActionManager().getAllPrototypeClones();
    for(Action oa: organismActions){
      actions.add(new EnvironmentAction(oa.getName()));
    }
    
    return actions;
  }

  @Override
  public Collection<EnvironmentStimulus> possibleStimuli() {
    Collection<EnvironmentStimulus> stimuli = new LinkedList<EnvironmentStimulus>();

    // Gets all the organism stimuli available, and convert them to environment stimuli
    Set<Stimulus> organismStimuliu = organism.getStimulationSubsystem().getStimulusManager().getAllPrototypeClones();
    for(Stimulus os: organismStimuliu){
      stimuli.add(new EnvironmentStimulus(os.getName(), os));
    }
    
    return stimuli;
  }

  @Override
  public void receiveStimulus(EnvironmentStimulus environmentStimulus,
      StimulationStatus status) {
    
    
    Stimulation stimulation = stimulus2Stimulation.get(environmentStimulus);
    
    // If the stimulation is present, we may update it.
    if(stimulation != null){
    
      // A direct conversion... not very elegant, but avoids hard-coding dependencies elsewhere.
      // (i.e., as it is, the simulator itself is independent of the organism implementation)
      Stimulation.StimulusStatus organismSStatus = null;
      switch(status){
        case ABSENT:
          organismSStatus = Stimulation.StimulusStatus.ABSENT;
          break;
        case BEGINNING:
          organismSStatus = Stimulation.StimulusStatus.BEGINNING;
          break;
        
        case ENDING:
          organismSStatus = Stimulation.StimulusStatus.ENDING;
          break;
          
        case STABLE:
          organismSStatus = Stimulation.StimulusStatus.STABLE;
          break;
      }
      
      // Modify the stimulation
      stimulation.setStatus(organismSStatus);
    }
    
    // If no corresponding stimulation was present, it means that the organism does not recognize
    // the given stimulus.
  }
  
  /**
   * Returns the <code>Organism</code> that this component holds. This can be useful for
   * the calculation of properties that need access to detailed structure of organisms.
   * 
   * @return the <code>Organism</code> that this component holds.
   */
  public Organism getOrganism(){
    return organism;
  }
  
  @Override
  public void receiveStimulus(EnvironmentStimulus environmentStimulus) {
    throw new UnsupportedOperationException("A stimulus must be delivered with an associate status.");
  }


  @Override
  public String toString() {
    return "[Organism " + id +" " + possibleActions().toString() + ", " + possibleStimuli().toString() + "]";
  }


  @Override
  public String affiliation() {
    return "none";
  }


  


}
