package edu.isi.bmkeg.ooevv.controller.impl;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.semanticweb.owlapi.model.OWLOntology;

import edu.isi.bmkeg.ooevv.controller.OoevvEngine;
import edu.isi.bmkeg.ooevv.dao.ExtendedOoevvDao;
import edu.isi.bmkeg.ooevv.dao.ExtendedOoevvDaoImpl;
import edu.isi.bmkeg.ooevv.model.ExperimentalVariable;
import edu.isi.bmkeg.ooevv.model.OoevvElement;
import edu.isi.bmkeg.ooevv.model.OoevvElementSet;
import edu.isi.bmkeg.ooevv.model.OoevvEntity;
import edu.isi.bmkeg.ooevv.model.OoevvProcess;
import edu.isi.bmkeg.ooevv.model.scale.BinaryScaleWithNamedValues;
import edu.isi.bmkeg.ooevv.model.scale.HierarchicalScale;
import edu.isi.bmkeg.ooevv.model.scale.MeasurementScale;
import edu.isi.bmkeg.ooevv.model.scale.NominalScaleWithAllowedTerms;
import edu.isi.bmkeg.ooevv.model.scale.OrdinalScaleWithNamedRanks;
import edu.isi.bmkeg.ooevv.model.scale.RelativeTermScale;
import edu.isi.bmkeg.ooevv.model.value.HierarchicalValue;
import edu.isi.bmkeg.ooevv.model.value.NominalValue;
import edu.isi.bmkeg.ooevv.model.value.OrdinalValue;
import edu.isi.bmkeg.terminology.model.Term;
import edu.isi.bmkeg.uml.interfaces.OwlUmlInterface;
import edu.isi.bmkeg.uml.model.UMLclass;
import edu.isi.bmkeg.uml.model.UMLmodel;
import edu.isi.bmkeg.uml.model.UMLrole;
import edu.isi.bmkeg.uml.utils.OwlAPIUtility;
import edu.isi.bmkeg.utils.superGraph.SuperGraphEdge;
import edu.isi.bmkeg.utils.superGraph.SuperGraphNode;
import edu.isi.bmkeg.utils.superGraph.SuperGraphTraversal;
import edu.isi.bmkeg.vpdmf.dao.CoreDao;
import edu.isi.bmkeg.vpdmf.model.definitions.PrimitiveDefinition;
import edu.isi.bmkeg.vpdmf.model.definitions.VPDMf;
import edu.isi.bmkeg.vpdmf.model.definitions.ViewDefinition;
import edu.isi.bmkeg.vpdmf.model.instances.AttributeInstance;
import edu.isi.bmkeg.vpdmf.model.instances.ClassInstance;
import edu.isi.bmkeg.vpdmf.model.instances.PrimitiveInstance;
import edu.isi.bmkeg.vpdmf.model.instances.PrimitiveInstanceGraph;
import edu.isi.bmkeg.vpdmf.model.instances.PrimitiveLinkInstance;
import edu.isi.bmkeg.vpdmf.model.instances.ViewBasedObjectGraph;
import edu.isi.bmkeg.vpdmf.model.instances.ViewInstance;

public class OoevvEngineImpl implements OoevvEngine {

	private OwlAPIUtility owlUtil;

	private ExtendedOoevvDao dao;
	
	public OoevvEngineImpl() throws Exception {
		this.dao = new ExtendedOoevvDaoImpl();
	}
	
	public OwlAPIUtility getOwlUtil() {
		return owlUtil;
	}

	public void setOwlUtil(OwlAPIUtility owlUtil) {
		this.owlUtil = owlUtil;
	}

	public ExtendedOoevvDao getDao() {
		return dao;
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~
	// OWL-based functionality
	// ~~~~~~~~~~~~~~~~~~~~~~~
	public OoevvElementSet saveAllOoevvElementSetToOwl(File owlFile, String uri)
			throws Exception {
	
		OWLOntology o = this.getOwlUtil().loadOntology(
				"http://bmkeg.isi.edu/ooevv/", owlFile.getAbsolutePath());
	
		this.getOwlUtil().addOntologyMetadata(o);
	
		this.getOwlUtil().setPrefix(uri);
	
		OoevvElementSet evs = new OoevvElementSet();
		Set<ViewInstance> exptVbs;
	
		CoreDao coreDao = this.dao.getCoreDao();
		
		try {
	
			coreDao.getCe().connectToDB();
	
			String viewName = "ExperimentalVariable";
			String attrAddr = "]OoevvElementSet|OoevvElementSet.name";
	
			ViewDefinition vd = coreDao.getTop().getViews().get(viewName);
			ViewInstance qVi = new ViewInstance(vd);
			List<ViewInstance> l = coreDao.getCe().executeFullQuery(qVi);
	
			Iterator<ViewInstance> vIt = l.iterator();
			while (vIt.hasNext()) {
				ViewInstance vi = vIt.next();
	
				this.saveViewInstanceToOntology(o, uri, vi);
	
				// ---------------------------------
				// read type of MeasurementScale
				// ---------------------------------
				attrAddr = "]MeasurementScale|MeasurementScale.classType";
				AttributeInstance ai = vi.readAttributeInstance(attrAddr, 0);
				String scaleClassType = ai.readValueString();
	
				attrAddr = "]MeasurementScale|MeasurementScale.bmkegId";
				ai = vi.readAttributeInstance(attrAddr, 0);
				String idStr = ai.readValueString();
	
				if (scaleClassType != null) {
	
					// ---------------------------------
					// query MeasurementScale
					// ---------------------------------
					ViewBasedObjectGraph vbog = dao.getVbogs().get(scaleClassType);
					l = coreDao.goGetHeavyViewList(scaleClassType, attrAddr, idStr);
					this.saveViewInstanceToOntology(o, uri, l.get(0));
	
				}
	
				this.getOwlUtil().saveOntology(o);
	
			}
	
		} catch (Exception e) {
	
			e.printStackTrace();
			throw e;
	
		} finally {
	
			coreDao.getCe().closeDbConnection();
	
		}
	
		return evs;
	
	}

	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	// Save data to OWL from Object Graph
	// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
	public void saveOoevvElementSetToOwl(OoevvElementSet evs, File owlFile,
			String uri) throws Exception {
	
		OWLOntology o = this.getOwlUtil().loadOntology(
				"http://bmkeg.isi.edu/ooevv/", owlFile.getAbsolutePath());
	
		this.getOwlUtil().addOntologyMetadata(o);
	
		this.getOwlUtil().setPrefix(uri);
	
		Set<ViewInstance> exptVbs;
		
		ViewBasedObjectGraph vbog = dao.getVbogs().get("OoevvElementSet");
		ViewInstance vi = vbog.objectGraphToView(evs);
		this.saveViewInstanceToOntology(o, uri, vi);
	
		Iterator<OoevvElement> exptVbIt = evs.getElements().iterator();
		while (exptVbIt.hasNext()) {
			OoevvElement el = exptVbIt.next();
			if( !(el instanceof ExperimentalVariable) ) {
				continue;
			}
			
			ExperimentalVariable ev = (ExperimentalVariable) el;
	
			vbog = dao.getVbogs().get("ExperimentalVariable");
			vi = vbog.objectGraphToView(ev);
			this.saveViewInstanceToOntology(o, uri, vi);
	
			// ---------------------------------
			// read type of MeasurementScale
			// ---------------------------------
			String attrAddr = "]MeasurementScale|MeasurementScale.classType";
			AttributeInstance ai = vi.readAttributeInstance(attrAddr, 0);
			String scaleClassType = ai.readValueString();
	
			MeasurementScale ms = ev.getScale();
	
			if (scaleClassType != null) {
				vbog = dao.getVbogs().get(scaleClassType);
				vi = vbog.objectGraphToView(ms);
				this.saveViewInstanceToOntology(o, uri, vi);
			}
	
			if (ms instanceof BinaryScaleWithNamedValues) {
	
				BinaryScaleWithNamedValues bswnv = (BinaryScaleWithNamedValues) ms;
	
				vbog = dao.getVbogs().get("BinaryValue");
	
				vi = vbog.objectGraphToView(bswnv.getTrueValue());
				this.saveViewInstanceToOntology(o, uri, vi);
	
				vi = vbog.objectGraphToView(bswnv.getFalseValue());
				this.saveViewInstanceToOntology(o, uri, vi);
	
			} else if (ms instanceof NominalScaleWithAllowedTerms) {
	
				NominalScaleWithAllowedTerms nswat = (NominalScaleWithAllowedTerms) ms;
				Iterator<NominalValue> tIt = nswat.getNominalValues()
						.iterator();
				while (tIt.hasNext()) {
					NominalValue nv = tIt.next();
					vbog = dao.getVbogs().get("NominalValue");
					vi = vbog.objectGraphToView(nv);
					this.saveViewInstanceToOntology(o, uri, vi);
				}
	
			} else if (ms instanceof OrdinalScaleWithNamedRanks) {
	
				OrdinalScaleWithNamedRanks pswnr = (OrdinalScaleWithNamedRanks) ms;
				Iterator<OrdinalValue> tIt = pswnr.getOrdinalValues()
						.iterator();
				while (tIt.hasNext()) {
					OrdinalValue ov = tIt.next();
					vbog = dao.getVbogs().get("OrdinalValue");
					vi = vbog.objectGraphToView(ov);
					this.saveViewInstanceToOntology(o, uri, vi);
				}
	
			} else if (ms instanceof RelativeTermScale) {
	
				/*
				 * Need to do some interesting work here... RelativeTermScale
				 * rts = (RelativeTermScale) ms; Iterator<Term> tIt =
				 * rts.getAllowedRelations().iterator(); while (tIt.hasNext()) {
				 * terms.add(tIt.next()); }
				 */
	
			} else if (ms instanceof HierarchicalScale) {
	
				HierarchicalScale hts = (HierarchicalScale) ms;
				Iterator<HierarchicalValue> tIt = hts.getHierarchicalValues()
						.iterator();
				while (tIt.hasNext()) {
					HierarchicalValue hv = tIt.next();
					vbog = dao.getVbogs().get("HierarchicalValue");
					vi = vbog.objectGraphToView(hv);
					this.saveViewInstanceToOntology(o, uri, vi);
				}
	
			} else if (ms instanceof HierarchicalScale) {
	
				HierarchicalScale hts = (HierarchicalScale) ms;
				Iterator<HierarchicalValue> tIt = hts.getHierarchicalValues()
						.iterator();
				while (tIt.hasNext()) {
					HierarchicalValue hv = tIt.next();
					vbog = dao.getVbogs().get("HierarchicalValue");
					vi = vbog.objectGraphToView(hv);
					this.saveViewInstanceToOntology(o, uri, vi);
				}
	
			}
	
		}
	
		Iterator<OoevvElement> procIt = evs.getElements().iterator();
		while (procIt.hasNext()) {
			OoevvElement el = procIt.next();
			if( !(el instanceof OoevvProcess) ) {
				continue;
			}
			OoevvProcess p = (OoevvProcess) el;
			vbog = dao.getVbogs().get("OoevvProcess");
			vi = vbog.objectGraphToView(p);
			this.saveViewInstanceToOntology(o, uri, vi);
		}
	
		Iterator<OoevvElement> entIt = evs.getElements().iterator();
		while (entIt.hasNext()) {
			OoevvElement el = entIt.next();
			if( !(el instanceof OoevvEntity) ) {
				continue;
			}
			OoevvEntity e = (OoevvEntity) el;
			vbog = dao.getVbogs().get("OoevvEntity");
			vi = vbog.objectGraphToView(e);
			this.saveViewInstanceToOntology(o, uri, vi);
		}
	
		this.getOwlUtil().saveOntology(o);
	
	}

	public void saveOoevvSystemAsOwl(File owlFile, String uri, String pkgPattern)
			throws Exception {
	
		VPDMf top = this.dao.getCoreDao().getTop();
		UMLmodel m = top.getUmlModel();
	
		if (owlFile.exists()) {
			owlFile.delete();
		}
	
		OwlUmlInterface oui = new OwlUmlInterface();
		oui.setUmlModel(m);
	
		m.cleanModel();
		oui.convertAttributes();
	
		OwlAPIUtility owlUtil = this.getOwlUtil();
		OWLOntology o = owlUtil.createOntology(uri, owlFile.getAbsolutePath());
		owlUtil.setPrefix(uri);
		owlUtil.addOntologyMetadata(o);
		
		Map<String, UMLclass> classMap = m.listClasses(pkgPattern);
	
		Set<String> toOmit = new HashSet<String>();
		toOmit.add("ViewTable");
		toOmit.add("ViewLinkTable");
		toOmit.add("vpdmfUser");
		toOmit.add("KnowledgeBase");
		toOmit.add("Person");
		toOmit.add("Term");
		toOmit.add("Ontology");
		toOmit.add("TermMapping");
	
		//
		// Add each class and name it.
		//
		Iterator<String> cIt = classMap.keySet().iterator();
		while (cIt.hasNext()) {
			String addr = cIt.next();
			UMLclass c = classMap.get(addr);
	
			addr = addr.substring(2, addr.length());
	
			// Check to see if the class is a set backing table...
			// if so don't generate the source code.
			if ( c.isDataType() || 
					(c.getStereotype() != null && c.getStereotype().equals("Link"))
					|| toOmit.contains(c.getBaseName())) {
				continue;
			}
	
			owlUtil.addClass(c.readClassAddress(), o);
			owlUtil.addNameComment(c.readClassAddress(), c.getBaseName(), o);
	
			String docs = c.getDocumentation();
			if (docs != null && docs.length() > 0) {
				owlUtil.addExternalAnnotation(c.readClassAddress(), "definition",
						docs, o);
			}
			
		}
	
		//
		// Add inheritance relationships.
		//
		cIt = classMap.keySet().iterator();
		while (cIt.hasNext()) {
			String addr = cIt.next();
			UMLclass c = classMap.get(addr);
	
			// Check to see if the class is a set backing table...
			// if so don't generate the source code.
			if ((c.getStereotype() != null && c.getStereotype().equals("Link"))
					|| toOmit.contains(c.getBaseName())) {
				continue;
			}
	
			UMLclass parent = c.getParent();
	
			if (parent != null) {
	
				if (toOmit.contains(parent.getBaseName())) {
					continue;
				}
	
				owlUtil.addSubClassToClass(parent.readClassAddress(),
						c.readClassAddress(), o);
	
			}
	
		}
	
		//
		// Add datatype & object type properties to classes from UML attributes
		// and roles.
		// - note that OWL uses universal definitions for properties.
		// They are not scoped to the enclosing class, so we will check the UML
		// definitions and
		// throw an exception if two properties have the same name and a
		// different class as it's
		// range.
		//
		cIt = classMap.keySet().iterator();
		while (cIt.hasNext()) {
			String addr = cIt.next();
			UMLclass c = classMap.get(addr);
	
			// Check to see if the class is a set backing table...
			// if so don't generate the source code.
			if ((c.getStereotype() != null && c.getStereotype().equals("Link"))
					|| toOmit.contains(c.getBaseName())) {
				continue;
			}
	
			owlUtil.constructAllRestrictionsForUMLClass(c, o);
	
		}
	
		owlUtil.constructAllDomainRestrictions(o);
	
		owlUtil.saveOntology(o);
	
	}

	public void savePrimitiveInstanceToOntology(OWLOntology o, String uri,
			PrimitiveInstance pi, Term t) throws Exception {
	
		OwlAPIUtility owlUtil = this.getOwlUtil();
	
		PrimitiveDefinition pd = pi.getDefinition();
	
		UMLclass umlClass = pd.getClasses().get(pd.getClasses().size() - 1);
	
		ClassInstance ci = pi.getObjects().get(umlClass.getBaseName());
		Iterator<AttributeInstance> aiIt = ci.getAttributes().values().iterator();
	
		String termValue = t.getTermValue();
		String shortTermId = t.getShortTermId();
		String definition = t.getDefinition();
	
		// Keep the representation only focussed on instances.
		// The main goal here is to create a minimal representation that is
		// *NOT*
		// a part of the OWL-based reasoning universe.
		// This code saves each element of the VPDMf representation as a class
		// not an instance.
		// owlUtil.addClass(shortTermId, o);
		// owlUtil.addNameComment(shortTermId, termValue, o);
		// owlUtil.addSubClassToClass(umlClass.readClassAddress(), shortTermId,
		// o);*/
	
		owlUtil.addIndividualToClass(umlClass.readClassAddress(), shortTermId,
				o);
		owlUtil.addNameComment(shortTermId, termValue, o);
	
		if (definition != null) {
			owlUtil.addExternalAnnotation(shortTermId, "definition",
					definition, o);
		}
	
	}

	public void saveViewInstanceToOntology(OWLOntology o, String uri,
			ViewInstance vi) throws Exception {
	
		OwlAPIUtility owlUtil = this.getOwlUtil();
	
		// construct lookup for terms based on primitives in the view
		// (this permits the system to recognize both OoEVV objects and external
		// terms).
		Map<PrimitiveInstance, Term> tLookup = buildTermLookupForViewInstance(vi);
	
		PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) vi.getSubGraph();
		SuperGraphTraversal pigTraversal = pig.readTraversal();
	
		Iterator<SuperGraphNode> piIt = pigTraversal.nodeTraversal.iterator();
		while (piIt.hasNext()) {
			PrimitiveInstance pi = (PrimitiveInstance) piIt.next();
	
			Term t = tLookup.get(pi);
			if (t == null || t.getShortTermId() == null)
				continue;
	
			this.savePrimitiveInstanceToOntology(o, uri, pi, t);
	
		}
	
		Iterator<SuperGraphEdge> pliIt = pigTraversal.edgeTraversal.iterator();
		while (pliIt.hasNext()) {
			PrimitiveLinkInstance pli = (PrimitiveLinkInstance) pliIt.next();
	
			UMLrole r = pli.getPVLinkDef().getRole();
			String dClassName = r.getDirectClass().getBaseName();
			PrimitiveInstance pi1 = (PrimitiveInstance) pli.getOutEdgeNode();
			PrimitiveInstance pi2 = (PrimitiveInstance) pli.getInEdgeNode();
	
			if (dClassName.equals("Ontology")
					|| dClassName.equals("TermMapping")
					|| dClassName.equals("Person"))
				continue;
	
			if (dClassName.equals("Term") && r.getBaseName().equals("term"))
				continue;
	
			// is the primitive link forward facing?
			boolean forward = true;
			if (pi2.getDefinition().lookupClassByName(
					r.getDirectClass().getBaseName()) == null) {
				forward = false;
			}
	
			PrimitiveInstance sPi = null, tPi = null;
			if (forward) {
				sPi = pi1;
				tPi = pi2;
			} else {
				sPi = pi2;
				tPi = pi1;
			}
	
			Term sTerm = tLookup.get(sPi);
			Term tTerm = tLookup.get(tPi);
	
			if (sTerm == null || tTerm == null
					|| sTerm.getShortTermId() == null
					|| tTerm.getShortTermId() == null)
				continue;
	
			owlUtil.addObjectPropertyToIndividual(sTerm.getShortTermId(),
					r.getBaseName(), tTerm.getShortTermId(), o);
	
		}
	
	}
	
	private Map<PrimitiveInstance, Term> buildTermLookupForViewInstance(
			ViewInstance vi) throws Exception {

		ViewBasedObjectGraph vbog = dao.getVbogs().get(vi.getDefName());
		vbog.viewToObjectGraph(vi);

		Map<PrimitiveInstance, Term> tLookup = new HashMap<PrimitiveInstance, Term>();
		PrimitiveInstanceGraph pig = (PrimitiveInstanceGraph) vi.getSubGraph();
		SuperGraphTraversal pigTraversal = pig.readTraversal();
		Iterator<SuperGraphEdge> pliIt = pigTraversal.edgeTraversal.iterator();
		while (pliIt.hasNext()) {
			PrimitiveLinkInstance pli = (PrimitiveLinkInstance) pliIt.next();

			UMLrole r = pli.getPVLinkDef().getRole();
			String dClassName = r.getDirectClass().getBaseName();

			if (dClassName.equals("Ontology")
					|| dClassName.equals("TermMapping")
					|| dClassName.equals("Person"))
				continue;

			if (dClassName.equals("Term")
					&& (r.getBaseName().equals("ontology") || r.getBaseName()
							.equals("definitionEditor")))
				continue;

			PrimitiveInstance pi1 = (PrimitiveInstance) pli.getOutEdgeNode();
			PrimitiveInstance pi2 = (PrimitiveInstance) pli.getInEdgeNode();

			PrimitiveInstance sPi = null, tPi = null;
			if (pi1.getDefinition().getPrimaryClass().getBaseName()
					.equals("Term")) {
				sPi = pi2;
				tPi = pi1;
			} else if (pi2.getDefinition().getPrimaryClass().getBaseName()
					.equals("Term")) {
				sPi = pi1;
				tPi = pi2;
			} else {
				continue;
			}

			Map<String, Object> objMap = vbog.getObjMap();
			Object o = objMap.get(tPi.getName());
			if (o == null)
				throw new Exception("Can't find primitive "
						+ tPi.getDefinition().getName());

			Term t = (Term) o;

			if (r.getBaseName().equals("term")) {
				tLookup.put(sPi, t);
			} else {
				tLookup.put(tPi, t);
			}

		}
		return tLookup;
	}

}