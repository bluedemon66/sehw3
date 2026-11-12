package edu.nju.sehw.takeout.client.logic;

import edu.nju.sehw.takeout.client.core.Core;
import edu.nju.sehw.takeout.client.core.MessageParticipant;
import edu.nju.sehw.takeout.client.processing.RequestContext;
import edu.nju.sehw.takeout.client.security.ClientPermissionChecker;
import edu.nju.sehw.takeout.model.message.Message;
import edu.nju.sehw.takeout.model.message.OperationRequest;
import edu.nju.sehw.takeout.scaffold.NotImplementedYetException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Translates a client business request into numbered data operations sent through Core. */
public final class CrudOperationService extends MessageParticipant {
  /** Permission preflight or server authorization service used by this component. */
  private final ClientPermissionChecker checker;

  /** Request-specific arguments and UI origins retained while operations are pending. */
  private final Map<UUID, RequestContext> contexts = new ConcurrentHashMap<>();

  /**
   * Creates this participant with the dependencies documented below.
   *
   * @param participantId positive identifier assigned to this participant in its local Core
   * @param core local Core that owns this participant and routes its messages
   * @param checker permission preflight or server authorization service used by this component
   * @throws NullPointerException if a required component, dependency, or collection element is null
   */
  public CrudOperationService(int participantId, Core core, ClientPermissionChecker checker) {
    super(participantId, core);
    this.checker = Objects.requireNonNull(checker, "checker");
  }

  /**
   * Plans the numbered data operations required by the incoming client business request.
   *
   * @param message message envelope containing request identity, routing fields, operation code,
   *     and arguments
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  @Override
  public void parse(Message message) {
    throw new NotImplementedYetException("CrudOperationService.parse");
  }

  /**
   * Selects the ordered data-operation plan for the business call identifier.
   *
   * @param callId numbered event or navigation action selected by the incoming message
   * @return the numbered operation requests to submit for that business call
   * @throws NotImplementedYetException P.S. This entry point is currently awaiting implementation.
   */
  private List<OperationRequest> planOperations(int callId) {
    throw new NotImplementedYetException("CrudOperationService.planOperations");
  }
}
