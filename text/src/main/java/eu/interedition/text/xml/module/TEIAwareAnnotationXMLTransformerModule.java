/*
 * #%L
 * Text: A text model with range-based markup via standoff annotations.
 * %%
 * Copyright (C) 2010 - 2011 The Interedition Development Group
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package eu.interedition.text.xml.module;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import eu.interedition.text.Name;
import eu.interedition.text.Range;
import eu.interedition.text.TextRepository;
import eu.interedition.text.mem.SimpleAnnotation;
import eu.interedition.text.mem.SimpleName;
import eu.interedition.text.xml.XMLEntity;
import eu.interedition.text.xml.XMLTransformer;
import org.codehaus.jackson.node.ObjectNode;

import java.util.Iterator;
import java.util.Map;

import static eu.interedition.text.TextConstants.TEI_NS;
import static eu.interedition.text.TextConstants.XML_ID_ATTR_NAME;

/**
 * @author <a href="http://gregor.middell.net/" title="Homepage">Gregor Middell</a>
 */
public class TEIAwareAnnotationXMLTransformerModule extends AbstractAnnotationXMLTransformerModule {
  private static final Map<Name, Name> MILESTONE_ELEMENT_UNITS = Maps.newHashMap();

  static {
    MILESTONE_ELEMENT_UNITS.put(new SimpleName(TEI_NS, "pb"), new SimpleName(TEI_NS, "page"));
    MILESTONE_ELEMENT_UNITS.put(new SimpleName(TEI_NS, "lb"), new SimpleName(TEI_NS, "line"));
    MILESTONE_ELEMENT_UNITS.put(new SimpleName(TEI_NS, "cb"), new SimpleName(TEI_NS, "column"));
    MILESTONE_ELEMENT_UNITS.put(new SimpleName(TEI_NS, "gb"), new SimpleName(TEI_NS, "gathering"));
  }

  private static final Name MILESTONE_NAME = new SimpleName(TEI_NS, "milestone");
  private static final Name MILESTONE_UNIT_ATTR_NAME = new SimpleName(TEI_NS, "unit");

  private Multimap<String, SimpleAnnotation> spanning;
  private Map<Name, SimpleAnnotation> milestones;

  public TEIAwareAnnotationXMLTransformerModule(int batchSize) {
    super(batchSize, false);
  }

  @Override
  public void start(XMLTransformer transformer) {
    super.start(transformer);
    this.spanning = ArrayListMultimap.create();
    this.milestones = Maps.newHashMap();
  }

  @Override
  public void end(XMLTransformer transformer) {
    final long textOffset = transformer.getTextOffset();
    for (Name milestoneUnit : milestones.keySet()) {
      final SimpleAnnotation last = milestones.get(milestoneUnit);
      add(transformer, last.getText(), last.getName(), new Range(last.getRange().getStart(), textOffset), last.getData());
    }

    this.milestones = null;
    this.spanning = null;

    super.end(transformer);
  }

  @Override
  public void start(XMLTransformer transformer, XMLEntity entity) {
    super.start(transformer, entity);
    handleSpanningElements(entity, transformer);
    handleMilestoneElements(entity, transformer);
  }

  protected void handleMilestoneElements(XMLEntity entity, XMLTransformer state) {
    final Name entityName = entity.getName();
    final ObjectNode entityAttributes = entity.getAttributes();

    Name milestoneUnit = null;
    if (MILESTONE_NAME.equals(entityName)) {
      for (Iterator<String> it = entityAttributes.getFieldNames(); it.hasNext(); ) {
        final String attrName = it.next();
        if (MILESTONE_UNIT_ATTR_NAME.getLocalName().equals(attrName) || MILESTONE_UNIT_ATTR_NAME.toString().equals(attrName)) {
          milestoneUnit = new SimpleName(TEI_NS, entityAttributes.get(attrName).toString());
          it.remove();
        }
      }
    } else if (MILESTONE_ELEMENT_UNITS.containsKey(entityName)) {
      milestoneUnit = MILESTONE_ELEMENT_UNITS.get(entityName);
    }

    if (milestoneUnit == null) {
      return;
    }

    final long textOffset = state.getTextOffset();

    final SimpleAnnotation last = milestones.get(milestoneUnit);
    if (last != null) {
      add(state, last.getText(), last.getName(), new Range(last.getRange().getStart(), textOffset), last.getData());
    }

    milestones.put(milestoneUnit, new SimpleAnnotation(state.getTarget(), milestoneUnit, new Range(textOffset, textOffset), entityAttributes));
  }

  protected void handleSpanningElements(XMLEntity entity, XMLTransformer state) {
    final ObjectNode entityAttributes = entity.getAttributes();
    String spanTo = null;
    String refId = null;
    for (Iterator<String> it = entityAttributes.getFieldNames(); it.hasNext(); ) {
      final String attrName = it.next();
      if (attrName.endsWith("spanTo")) {
        spanTo = entityAttributes.get(attrName).toString().replaceAll("^#", "");
        it.remove();
      } else if (XML_ID_ATTR_NAME.toString().equals(attrName)) {
        refId = entityAttributes.get(attrName).toString();
      }
    }

    if (spanTo == null && refId == null) {
      return;
    }

    final long textOffset = state.getTextOffset();

    if (spanTo != null) {
      final Name name = entity.getName();
      spanning.put(spanTo, new SimpleAnnotation(
              state.getTarget(),
              new SimpleName(name.getNamespace(), name.getLocalName().replaceAll("Span$", "")),
              new Range(textOffset, textOffset),
              entityAttributes));
    }
    if (refId != null) {
      final Iterator<SimpleAnnotation> aIt = spanning.removeAll(refId).iterator();
      while (aIt.hasNext()) {
        final SimpleAnnotation a = aIt.next();
        add(state, a.getText(), a.getName(), new Range(a.getRange().getStart(), textOffset), a.getData());
      }
    }
  }
}