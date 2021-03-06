##################################################
# file: ContentConfigurationServiceImplService_types.py
#
# schema types generated by "ZSI.generate.wsdl2python.WriteServiceModule"
#    /usr/bin/wsdl2py -b ContentConfigurationService.wsdl
#
##################################################

import ZSI
import ZSI.TCcompound
from ZSI.schema import LocalElementDeclaration, ElementDeclaration, TypeDefinition, GTD, GED
from ZSI.generate.pyclass import pyclass_type

##############################
# targetNamespace
# http://content.ws.lockss.org/
##############################

class ns0:
    targetNamespace = "http://content.ws.lockss.org/"

    class reactivateAusByIdList_Def(ZSI.TCcompound.ComplexType, TypeDefinition):
        schema = "http://content.ws.lockss.org/"
        type = (schema, "reactivateAusByIdList")
        def __init__(self, pname, ofwhat=(), attributes=None, extend=False, restrict=False, **kw):
            ns = ns0.reactivateAusByIdList_Def.schema
            TClist = [ZSI.TC.String(pname="auIds", aname="_auIds", minOccurs=0, maxOccurs="unbounded", nillable=False, typed=False, encoded=kw.get("encoded"))]
            self.attribute_typecode_dict = attributes or {}
            if extend: TClist += ofwhat
            if restrict: TClist = ofwhat
            ZSI.TCcompound.ComplexType.__init__(self, None, TClist, pname=pname, inorder=0, **kw)
            class Holder:
                __metaclass__ = pyclass_type
                typecode = self
                def __init__(self):
                    # pyclass
                    self._auIds = []
                    return
            Holder.__name__ = "reactivateAusByIdList_Holder"
            self.pyclass = Holder

    class reactivateAusByIdListResponse_Def(ZSI.TCcompound.ComplexType, TypeDefinition):
        schema = "http://content.ws.lockss.org/"
        type = (schema, "reactivateAusByIdListResponse")
        def __init__(self, pname, ofwhat=(), attributes=None, extend=False, restrict=False, **kw):
            ns = ns0.reactivateAusByIdListResponse_Def.schema
            TClist = [GTD("http://content.ws.lockss.org/","contentConfigurationResult",lazy=False)(pname="return", aname="_return", minOccurs=0, maxOccurs="unbounded", nillable=False, typed=False, encoded=kw.get("encoded"))]
            self.attribute_typecode_dict = attributes or {}
            if extend: TClist += ofwhat
            if restrict: TClist = ofwhat
            ZSI.TCcompound.ComplexType.__init__(self, None, TClist, pname=pname, inorder=0, **kw)
            class Holder:
                __metaclass__ = pyclass_type
                typecode = self
                def __init__(self):
                    # pyclass
                    self._return = []
                    return
            Holder.__name__ = "reactivateAusByIdListResponse_Holder"
            self.pyclass = Holder

    class contentConfigurationResult_Def(ZSI.TCcompound.ComplexType, TypeDefinition):
        schema = "http://content.ws.lockss.org/"
        type = (schema, "contentConfigurationResult")
        def __init__(self, pname, ofwhat=(), attributes=None, extend=False, restrict=False, **kw):
            ns = ns0.contentConfigurationResult_Def.schema
            TClist = [ZSI.TC.String(pname="id", aname="_id", minOccurs=0, maxOccurs=1, nillable=False, typed=False, encoded=kw.get("encoded")), ZSI.TC.Boolean(pname="isSuccess", aname="_isSuccess", minOccurs=0, maxOccurs=1, nillable=False, typed=False, encoded=kw.get("encoded")), ZSI.TC.String(pname="message", aname="_message", minOccurs=0, maxOccurs=1, nillable=False, typed=False, encoded=kw.get("encoded")), ZSI.TC.String(pname="name", aname="_name", minOccurs=0, maxOccurs=1, nillable=False, typed=False, encoded=kw.get("encoded"))]
            self.attribute_typecode_dict = attributes or {}
            if extend: TClist += ofwhat
            if restrict: TClist = ofwhat
            ZSI.TCcompound.ComplexType.__init__(self, None, TClist, pname=pname, inorder=0, **kw)
            class Holder:
                __metaclass__ = pyclass_type
                typecode = self
                def __init__(self):
                    # pyclass
                    self._id = None
                    self._isSuccess = None
                    self._message = None
                    self._name = None
                    return
            Holder.__name__ = "contentConfigurationResult_Holder"
            self.pyclass = Holder

    class lockssWebServicesFaultInfo_Def(ZSI.TCcompound.ComplexType, TypeDefinition):
        schema = "http://content.ws.lockss.org/"
        type = (schema, "lockssWebServicesFaultInfo")
        def __init__(self, pname, ofwhat=(), attributes=None, extend=False, restrict=False, **kw):
            ns = ns0.lockssWebServicesFaultInfo_Def.schema
            TClist = [ZSI.TC.String(pname="message", aname="_message", minOccurs=0, maxOccurs=1, nillable=False, typed=False, encoded=kw.get("encoded"))]
            self.attribute_typecode_dict = attributes or {}
            if extend: TClist += ofwhat
            if restrict: TClist = ofwhat
            ZSI.TCcompound.ComplexType.__init__(self, None, TClist, pname=pname, inorder=0, **kw)
            class Holder:
                __metaclass__ = pyclass_type
                typecode = self
                def __init__(self):
                    # pyclass
                    self._message = None
                    return
            Holder.__name__ = "lockssWebServicesFaultInfo_Holder"
            self.pyclass = Holder

    class deleteAusByIdList_Def(ZSI.TCcompound.ComplexType, TypeDefinition):
        schema = "http://content.ws.lockss.org/"
        type = (schema, "deleteAusByIdList")
        def __init__(self, pname, ofwhat=(), attributes=None, extend=False, restrict=False, **kw):
            ns = ns0.deleteAusByIdList_Def.schema
            TClist = [ZSI.TC.String(pname="auIds", aname="_auIds", minOccurs=0, maxOccurs="unbounded", nillable=False, typed=False, encoded=kw.get("encoded"))]
            self.attribute_typecode_dict = attributes or {}
            if extend: TClist += ofwhat
            if restrict: TClist = ofwhat
            ZSI.TCcompound.ComplexType.__init__(self, None, TClist, pname=pname, inorder=0, **kw)
            class Holder:
                __metaclass__ = pyclass_type
                typecode = self
                def __init__(self):
                    # pyclass
                    self._auIds = []
                    return
            Holder.__name__ = "deleteAusByIdList_Holder"
            self.pyclass = Holder

    class deleteAusByIdListResponse_Def(ZSI.TCcompound.ComplexType, TypeDefinition):
        schema = "http://content.ws.lockss.org/"
        type = (schema, "deleteAusByIdListResponse")
        def __init__(self, pname, ofwhat=(), attributes=None, extend=False, restrict=False, **kw):
            ns = ns0.deleteAusByIdListResponse_Def.schema
            TClist = [GTD("http://content.ws.lockss.org/","contentConfigurationResult",lazy=False)(pname="return", aname="_return", minOccurs=0, maxOccurs="unbounded", nillable=False, typed=False, encoded=kw.get("encoded"))]
            self.attribute_typecode_dict = attributes or {}
            if extend: TClist += ofwhat
            if restrict: TClist = ofwhat
            ZSI.TCcompound.ComplexType.__init__(self, None, TClist, pname=pname, inorder=0, **kw)
            class Holder:
                __metaclass__ = pyclass_type
                typecode = self
                def __init__(self):
                    # pyclass
                    self._return = []
                    return
            Holder.__name__ = "deleteAusByIdListResponse_Holder"
            self.pyclass = Holder

    class deleteAuById_Def(ZSI.TCcompound.ComplexType, TypeDefinition):
        schema = "http://content.ws.lockss.org/"
        type = (schema, "deleteAuById")
        def __init__(self, pname, ofwhat=(), attributes=None, extend=False, restrict=False, **kw):
            ns = ns0.deleteAuById_Def.schema
            TClist = [ZSI.TC.String(pname="auId", aname="_auId", minOccurs=0, maxOccurs=1, nillable=False, typed=False, encoded=kw.get("encoded"))]
            self.attribute_typecode_dict = attributes or {}
            if extend: TClist += ofwhat
            if restrict: TClist = ofwhat
            ZSI.TCcompound.ComplexType.__init__(self, None, TClist, pname=pname, inorder=0, **kw)
            class Holder:
                __metaclass__ = pyclass_type
                typecode = self
                def __init__(self):
                    # pyclass
                    self._auId = None
                    return
            Holder.__name__ = "deleteAuById_Holder"
            self.pyclass = Holder

    class deleteAuByIdResponse_Def(ZSI.TCcompound.ComplexType, TypeDefinition):
        schema = "http://content.ws.lockss.org/"
        type = (schema, "deleteAuByIdResponse")
        def __init__(self, pname, ofwhat=(), attributes=None, extend=False, restrict=False, **kw):
            ns = ns0.deleteAuByIdResponse_Def.schema
            TClist = [GTD("http://content.ws.lockss.org/","contentConfigurationResult",lazy=False)(pname="return", aname="_return", minOccurs=0, maxOccurs=1, nillable=False, typed=False, encoded=kw.get("encoded"))]
            self.attribute_typecode_dict = attributes or {}
            if extend: TClist += ofwhat
            if restrict: TClist = ofwhat
            ZSI.TCcompound.ComplexType.__init__(self, None, TClist, pname=pname, inorder=0, **kw)
            class Holder:
                __metaclass__ = pyclass_type
                typecode = self
                def __init__(self):
                    # pyclass
                    self._return = None
                    return
            Holder.__name__ = "deleteAuByIdResponse_Holder"
            self.pyclass = Holder

    class reactivateAuById_Def(ZSI.TCcompound.ComplexType, TypeDefinition):
        schema = "http://content.ws.lockss.org/"
        type = (schema, "reactivateAuById")
        def __init__(self, pname, ofwhat=(), attributes=None, extend=False, restrict=False, **kw):
            ns = ns0.reactivateAuById_Def.schema
            TClist = [ZSI.TC.String(pname="auId", aname="_auId", minOccurs=0, maxOccurs=1, nillable=False, typed=False, encoded=kw.get("encoded"))]
            self.attribute_typecode_dict = attributes or {}
            if extend: TClist += ofwhat
            if restrict: TClist = ofwhat
            ZSI.TCcompound.ComplexType.__init__(self, None, TClist, pname=pname, inorder=0, **kw)
            class Holder:
                __metaclass__ = pyclass_type
                typecode = self
                def __init__(self):
                    # pyclass
                    self._auId = None
                    return
            Holder.__name__ = "reactivateAuById_Holder"
            self.pyclass = Holder

    class reactivateAuByIdResponse_Def(ZSI.TCcompound.ComplexType, TypeDefinition):
        schema = "http://content.ws.lockss.org/"
        type = (schema, "reactivateAuByIdResponse")
        def __init__(self, pname, ofwhat=(), attributes=None, extend=False, restrict=False, **kw):
            ns = ns0.reactivateAuByIdResponse_Def.schema
            TClist = [GTD("http://content.ws.lockss.org/","contentConfigurationResult",lazy=False)(pname="return", aname="_return", minOccurs=0, maxOccurs=1, nillable=False, typed=False, encoded=kw.get("encoded"))]
            self.attribute_typecode_dict = attributes or {}
            if extend: TClist += ofwhat
            if restrict: TClist = ofwhat
            ZSI.TCcompound.ComplexType.__init__(self, None, TClist, pname=pname, inorder=0, **kw)
            class Holder:
                __metaclass__ = pyclass_type
                typecode = self
                def __init__(self):
                    # pyclass
                    self._return = None
                    return
            Holder.__name__ = "reactivateAuByIdResponse_Holder"
            self.pyclass = Holder

    class addAuById_Def(ZSI.TCcompound.ComplexType, TypeDefinition):
        schema = "http://content.ws.lockss.org/"
        type = (schema, "addAuById")
        def __init__(self, pname, ofwhat=(), attributes=None, extend=False, restrict=False, **kw):
            ns = ns0.addAuById_Def.schema
            TClist = [ZSI.TC.String(pname="auId", aname="_auId", minOccurs=0, maxOccurs=1, nillable=False, typed=False, encoded=kw.get("encoded"))]
            self.attribute_typecode_dict = attributes or {}
            if extend: TClist += ofwhat
            if restrict: TClist = ofwhat
            ZSI.TCcompound.ComplexType.__init__(self, None, TClist, pname=pname, inorder=0, **kw)
            class Holder:
                __metaclass__ = pyclass_type
                typecode = self
                def __init__(self):
                    # pyclass
                    self._auId = None
                    return
            Holder.__name__ = "addAuById_Holder"
            self.pyclass = Holder

    class addAuByIdResponse_Def(ZSI.TCcompound.ComplexType, TypeDefinition):
        schema = "http://content.ws.lockss.org/"
        type = (schema, "addAuByIdResponse")
        def __init__(self, pname, ofwhat=(), attributes=None, extend=False, restrict=False, **kw):
            ns = ns0.addAuByIdResponse_Def.schema
            TClist = [GTD("http://content.ws.lockss.org/","contentConfigurationResult",lazy=False)(pname="return", aname="_return", minOccurs=0, maxOccurs=1, nillable=False, typed=False, encoded=kw.get("encoded"))]
            self.attribute_typecode_dict = attributes or {}
            if extend: TClist += ofwhat
            if restrict: TClist = ofwhat
            ZSI.TCcompound.ComplexType.__init__(self, None, TClist, pname=pname, inorder=0, **kw)
            class Holder:
                __metaclass__ = pyclass_type
                typecode = self
                def __init__(self):
                    # pyclass
                    self._return = None
                    return
            Holder.__name__ = "addAuByIdResponse_Holder"
            self.pyclass = Holder

    class deactivateAuById_Def(ZSI.TCcompound.ComplexType, TypeDefinition):
        schema = "http://content.ws.lockss.org/"
        type = (schema, "deactivateAuById")
        def __init__(self, pname, ofwhat=(), attributes=None, extend=False, restrict=False, **kw):
            ns = ns0.deactivateAuById_Def.schema
            TClist = [ZSI.TC.String(pname="auId", aname="_auId", minOccurs=0, maxOccurs=1, nillable=False, typed=False, encoded=kw.get("encoded"))]
            self.attribute_typecode_dict = attributes or {}
            if extend: TClist += ofwhat
            if restrict: TClist = ofwhat
            ZSI.TCcompound.ComplexType.__init__(self, None, TClist, pname=pname, inorder=0, **kw)
            class Holder:
                __metaclass__ = pyclass_type
                typecode = self
                def __init__(self):
                    # pyclass
                    self._auId = None
                    return
            Holder.__name__ = "deactivateAuById_Holder"
            self.pyclass = Holder

    class deactivateAuByIdResponse_Def(ZSI.TCcompound.ComplexType, TypeDefinition):
        schema = "http://content.ws.lockss.org/"
        type = (schema, "deactivateAuByIdResponse")
        def __init__(self, pname, ofwhat=(), attributes=None, extend=False, restrict=False, **kw):
            ns = ns0.deactivateAuByIdResponse_Def.schema
            TClist = [GTD("http://content.ws.lockss.org/","contentConfigurationResult",lazy=False)(pname="return", aname="_return", minOccurs=0, maxOccurs=1, nillable=False, typed=False, encoded=kw.get("encoded"))]
            self.attribute_typecode_dict = attributes or {}
            if extend: TClist += ofwhat
            if restrict: TClist = ofwhat
            ZSI.TCcompound.ComplexType.__init__(self, None, TClist, pname=pname, inorder=0, **kw)
            class Holder:
                __metaclass__ = pyclass_type
                typecode = self
                def __init__(self):
                    # pyclass
                    self._return = None
                    return
            Holder.__name__ = "deactivateAuByIdResponse_Holder"
            self.pyclass = Holder

    class addAusByIdList_Def(ZSI.TCcompound.ComplexType, TypeDefinition):
        schema = "http://content.ws.lockss.org/"
        type = (schema, "addAusByIdList")
        def __init__(self, pname, ofwhat=(), attributes=None, extend=False, restrict=False, **kw):
            ns = ns0.addAusByIdList_Def.schema
            TClist = [ZSI.TC.String(pname="auIds", aname="_auIds", minOccurs=0, maxOccurs="unbounded", nillable=False, typed=False, encoded=kw.get("encoded"))]
            self.attribute_typecode_dict = attributes or {}
            if extend: TClist += ofwhat
            if restrict: TClist = ofwhat
            ZSI.TCcompound.ComplexType.__init__(self, None, TClist, pname=pname, inorder=0, **kw)
            class Holder:
                __metaclass__ = pyclass_type
                typecode = self
                def __init__(self):
                    # pyclass
                    self._auIds = []
                    return
            Holder.__name__ = "addAusByIdList_Holder"
            self.pyclass = Holder

    class addAusByIdListResponse_Def(ZSI.TCcompound.ComplexType, TypeDefinition):
        schema = "http://content.ws.lockss.org/"
        type = (schema, "addAusByIdListResponse")
        def __init__(self, pname, ofwhat=(), attributes=None, extend=False, restrict=False, **kw):
            ns = ns0.addAusByIdListResponse_Def.schema
            TClist = [GTD("http://content.ws.lockss.org/","contentConfigurationResult",lazy=False)(pname="return", aname="_return", minOccurs=0, maxOccurs="unbounded", nillable=False, typed=False, encoded=kw.get("encoded"))]
            self.attribute_typecode_dict = attributes or {}
            if extend: TClist += ofwhat
            if restrict: TClist = ofwhat
            ZSI.TCcompound.ComplexType.__init__(self, None, TClist, pname=pname, inorder=0, **kw)
            class Holder:
                __metaclass__ = pyclass_type
                typecode = self
                def __init__(self):
                    # pyclass
                    self._return = []
                    return
            Holder.__name__ = "addAusByIdListResponse_Holder"
            self.pyclass = Holder

    class deactivateAusByIdList_Def(ZSI.TCcompound.ComplexType, TypeDefinition):
        schema = "http://content.ws.lockss.org/"
        type = (schema, "deactivateAusByIdList")
        def __init__(self, pname, ofwhat=(), attributes=None, extend=False, restrict=False, **kw):
            ns = ns0.deactivateAusByIdList_Def.schema
            TClist = [ZSI.TC.String(pname="auIds", aname="_auIds", minOccurs=0, maxOccurs="unbounded", nillable=False, typed=False, encoded=kw.get("encoded"))]
            self.attribute_typecode_dict = attributes or {}
            if extend: TClist += ofwhat
            if restrict: TClist = ofwhat
            ZSI.TCcompound.ComplexType.__init__(self, None, TClist, pname=pname, inorder=0, **kw)
            class Holder:
                __metaclass__ = pyclass_type
                typecode = self
                def __init__(self):
                    # pyclass
                    self._auIds = []
                    return
            Holder.__name__ = "deactivateAusByIdList_Holder"
            self.pyclass = Holder

    class deactivateAusByIdListResponse_Def(ZSI.TCcompound.ComplexType, TypeDefinition):
        schema = "http://content.ws.lockss.org/"
        type = (schema, "deactivateAusByIdListResponse")
        def __init__(self, pname, ofwhat=(), attributes=None, extend=False, restrict=False, **kw):
            ns = ns0.deactivateAusByIdListResponse_Def.schema
            TClist = [GTD("http://content.ws.lockss.org/","contentConfigurationResult",lazy=False)(pname="return", aname="_return", minOccurs=0, maxOccurs="unbounded", nillable=False, typed=False, encoded=kw.get("encoded"))]
            self.attribute_typecode_dict = attributes or {}
            if extend: TClist += ofwhat
            if restrict: TClist = ofwhat
            ZSI.TCcompound.ComplexType.__init__(self, None, TClist, pname=pname, inorder=0, **kw)
            class Holder:
                __metaclass__ = pyclass_type
                typecode = self
                def __init__(self):
                    # pyclass
                    self._return = []
                    return
            Holder.__name__ = "deactivateAusByIdListResponse_Holder"
            self.pyclass = Holder

    class addAuById_Dec(ElementDeclaration):
        literal = "addAuById"
        schema = "http://content.ws.lockss.org/"
        substitutionGroup = None
        def __init__(self, **kw):
            kw["pname"] = ("http://content.ws.lockss.org/","addAuById")
            kw["aname"] = "_addAuById"
            if ns0.addAuById_Def not in ns0.addAuById_Dec.__bases__:
                bases = list(ns0.addAuById_Dec.__bases__)
                bases.insert(0, ns0.addAuById_Def)
                ns0.addAuById_Dec.__bases__ = tuple(bases)

            ns0.addAuById_Def.__init__(self, **kw)
            if self.pyclass is not None: self.pyclass.__name__ = "addAuById_Dec_Holder"

    class addAuByIdResponse_Dec(ElementDeclaration):
        literal = "addAuByIdResponse"
        schema = "http://content.ws.lockss.org/"
        substitutionGroup = None
        def __init__(self, **kw):
            kw["pname"] = ("http://content.ws.lockss.org/","addAuByIdResponse")
            kw["aname"] = "_addAuByIdResponse"
            if ns0.addAuByIdResponse_Def not in ns0.addAuByIdResponse_Dec.__bases__:
                bases = list(ns0.addAuByIdResponse_Dec.__bases__)
                bases.insert(0, ns0.addAuByIdResponse_Def)
                ns0.addAuByIdResponse_Dec.__bases__ = tuple(bases)

            ns0.addAuByIdResponse_Def.__init__(self, **kw)
            if self.pyclass is not None: self.pyclass.__name__ = "addAuByIdResponse_Dec_Holder"

    class addAusByIdList_Dec(ElementDeclaration):
        literal = "addAusByIdList"
        schema = "http://content.ws.lockss.org/"
        substitutionGroup = None
        def __init__(self, **kw):
            kw["pname"] = ("http://content.ws.lockss.org/","addAusByIdList")
            kw["aname"] = "_addAusByIdList"
            if ns0.addAusByIdList_Def not in ns0.addAusByIdList_Dec.__bases__:
                bases = list(ns0.addAusByIdList_Dec.__bases__)
                bases.insert(0, ns0.addAusByIdList_Def)
                ns0.addAusByIdList_Dec.__bases__ = tuple(bases)

            ns0.addAusByIdList_Def.__init__(self, **kw)
            if self.pyclass is not None: self.pyclass.__name__ = "addAusByIdList_Dec_Holder"

    class addAusByIdListResponse_Dec(ElementDeclaration):
        literal = "addAusByIdListResponse"
        schema = "http://content.ws.lockss.org/"
        substitutionGroup = None
        def __init__(self, **kw):
            kw["pname"] = ("http://content.ws.lockss.org/","addAusByIdListResponse")
            kw["aname"] = "_addAusByIdListResponse"
            if ns0.addAusByIdListResponse_Def not in ns0.addAusByIdListResponse_Dec.__bases__:
                bases = list(ns0.addAusByIdListResponse_Dec.__bases__)
                bases.insert(0, ns0.addAusByIdListResponse_Def)
                ns0.addAusByIdListResponse_Dec.__bases__ = tuple(bases)

            ns0.addAusByIdListResponse_Def.__init__(self, **kw)
            if self.pyclass is not None: self.pyclass.__name__ = "addAusByIdListResponse_Dec_Holder"

    class deactivateAuById_Dec(ElementDeclaration):
        literal = "deactivateAuById"
        schema = "http://content.ws.lockss.org/"
        substitutionGroup = None
        def __init__(self, **kw):
            kw["pname"] = ("http://content.ws.lockss.org/","deactivateAuById")
            kw["aname"] = "_deactivateAuById"
            if ns0.deactivateAuById_Def not in ns0.deactivateAuById_Dec.__bases__:
                bases = list(ns0.deactivateAuById_Dec.__bases__)
                bases.insert(0, ns0.deactivateAuById_Def)
                ns0.deactivateAuById_Dec.__bases__ = tuple(bases)

            ns0.deactivateAuById_Def.__init__(self, **kw)
            if self.pyclass is not None: self.pyclass.__name__ = "deactivateAuById_Dec_Holder"

    class deactivateAuByIdResponse_Dec(ElementDeclaration):
        literal = "deactivateAuByIdResponse"
        schema = "http://content.ws.lockss.org/"
        substitutionGroup = None
        def __init__(self, **kw):
            kw["pname"] = ("http://content.ws.lockss.org/","deactivateAuByIdResponse")
            kw["aname"] = "_deactivateAuByIdResponse"
            if ns0.deactivateAuByIdResponse_Def not in ns0.deactivateAuByIdResponse_Dec.__bases__:
                bases = list(ns0.deactivateAuByIdResponse_Dec.__bases__)
                bases.insert(0, ns0.deactivateAuByIdResponse_Def)
                ns0.deactivateAuByIdResponse_Dec.__bases__ = tuple(bases)

            ns0.deactivateAuByIdResponse_Def.__init__(self, **kw)
            if self.pyclass is not None: self.pyclass.__name__ = "deactivateAuByIdResponse_Dec_Holder"

    class deactivateAusByIdList_Dec(ElementDeclaration):
        literal = "deactivateAusByIdList"
        schema = "http://content.ws.lockss.org/"
        substitutionGroup = None
        def __init__(self, **kw):
            kw["pname"] = ("http://content.ws.lockss.org/","deactivateAusByIdList")
            kw["aname"] = "_deactivateAusByIdList"
            if ns0.deactivateAusByIdList_Def not in ns0.deactivateAusByIdList_Dec.__bases__:
                bases = list(ns0.deactivateAusByIdList_Dec.__bases__)
                bases.insert(0, ns0.deactivateAusByIdList_Def)
                ns0.deactivateAusByIdList_Dec.__bases__ = tuple(bases)

            ns0.deactivateAusByIdList_Def.__init__(self, **kw)
            if self.pyclass is not None: self.pyclass.__name__ = "deactivateAusByIdList_Dec_Holder"

    class deactivateAusByIdListResponse_Dec(ElementDeclaration):
        literal = "deactivateAusByIdListResponse"
        schema = "http://content.ws.lockss.org/"
        substitutionGroup = None
        def __init__(self, **kw):
            kw["pname"] = ("http://content.ws.lockss.org/","deactivateAusByIdListResponse")
            kw["aname"] = "_deactivateAusByIdListResponse"
            if ns0.deactivateAusByIdListResponse_Def not in ns0.deactivateAusByIdListResponse_Dec.__bases__:
                bases = list(ns0.deactivateAusByIdListResponse_Dec.__bases__)
                bases.insert(0, ns0.deactivateAusByIdListResponse_Def)
                ns0.deactivateAusByIdListResponse_Dec.__bases__ = tuple(bases)

            ns0.deactivateAusByIdListResponse_Def.__init__(self, **kw)
            if self.pyclass is not None: self.pyclass.__name__ = "deactivateAusByIdListResponse_Dec_Holder"

    class deleteAuById_Dec(ElementDeclaration):
        literal = "deleteAuById"
        schema = "http://content.ws.lockss.org/"
        substitutionGroup = None
        def __init__(self, **kw):
            kw["pname"] = ("http://content.ws.lockss.org/","deleteAuById")
            kw["aname"] = "_deleteAuById"
            if ns0.deleteAuById_Def not in ns0.deleteAuById_Dec.__bases__:
                bases = list(ns0.deleteAuById_Dec.__bases__)
                bases.insert(0, ns0.deleteAuById_Def)
                ns0.deleteAuById_Dec.__bases__ = tuple(bases)

            ns0.deleteAuById_Def.__init__(self, **kw)
            if self.pyclass is not None: self.pyclass.__name__ = "deleteAuById_Dec_Holder"

    class deleteAuByIdResponse_Dec(ElementDeclaration):
        literal = "deleteAuByIdResponse"
        schema = "http://content.ws.lockss.org/"
        substitutionGroup = None
        def __init__(self, **kw):
            kw["pname"] = ("http://content.ws.lockss.org/","deleteAuByIdResponse")
            kw["aname"] = "_deleteAuByIdResponse"
            if ns0.deleteAuByIdResponse_Def not in ns0.deleteAuByIdResponse_Dec.__bases__:
                bases = list(ns0.deleteAuByIdResponse_Dec.__bases__)
                bases.insert(0, ns0.deleteAuByIdResponse_Def)
                ns0.deleteAuByIdResponse_Dec.__bases__ = tuple(bases)

            ns0.deleteAuByIdResponse_Def.__init__(self, **kw)
            if self.pyclass is not None: self.pyclass.__name__ = "deleteAuByIdResponse_Dec_Holder"

    class deleteAusByIdList_Dec(ElementDeclaration):
        literal = "deleteAusByIdList"
        schema = "http://content.ws.lockss.org/"
        substitutionGroup = None
        def __init__(self, **kw):
            kw["pname"] = ("http://content.ws.lockss.org/","deleteAusByIdList")
            kw["aname"] = "_deleteAusByIdList"
            if ns0.deleteAusByIdList_Def not in ns0.deleteAusByIdList_Dec.__bases__:
                bases = list(ns0.deleteAusByIdList_Dec.__bases__)
                bases.insert(0, ns0.deleteAusByIdList_Def)
                ns0.deleteAusByIdList_Dec.__bases__ = tuple(bases)

            ns0.deleteAusByIdList_Def.__init__(self, **kw)
            if self.pyclass is not None: self.pyclass.__name__ = "deleteAusByIdList_Dec_Holder"

    class deleteAusByIdListResponse_Dec(ElementDeclaration):
        literal = "deleteAusByIdListResponse"
        schema = "http://content.ws.lockss.org/"
        substitutionGroup = None
        def __init__(self, **kw):
            kw["pname"] = ("http://content.ws.lockss.org/","deleteAusByIdListResponse")
            kw["aname"] = "_deleteAusByIdListResponse"
            if ns0.deleteAusByIdListResponse_Def not in ns0.deleteAusByIdListResponse_Dec.__bases__:
                bases = list(ns0.deleteAusByIdListResponse_Dec.__bases__)
                bases.insert(0, ns0.deleteAusByIdListResponse_Def)
                ns0.deleteAusByIdListResponse_Dec.__bases__ = tuple(bases)

            ns0.deleteAusByIdListResponse_Def.__init__(self, **kw)
            if self.pyclass is not None: self.pyclass.__name__ = "deleteAusByIdListResponse_Dec_Holder"

    class reactivateAuById_Dec(ElementDeclaration):
        literal = "reactivateAuById"
        schema = "http://content.ws.lockss.org/"
        substitutionGroup = None
        def __init__(self, **kw):
            kw["pname"] = ("http://content.ws.lockss.org/","reactivateAuById")
            kw["aname"] = "_reactivateAuById"
            if ns0.reactivateAuById_Def not in ns0.reactivateAuById_Dec.__bases__:
                bases = list(ns0.reactivateAuById_Dec.__bases__)
                bases.insert(0, ns0.reactivateAuById_Def)
                ns0.reactivateAuById_Dec.__bases__ = tuple(bases)

            ns0.reactivateAuById_Def.__init__(self, **kw)
            if self.pyclass is not None: self.pyclass.__name__ = "reactivateAuById_Dec_Holder"

    class reactivateAuByIdResponse_Dec(ElementDeclaration):
        literal = "reactivateAuByIdResponse"
        schema = "http://content.ws.lockss.org/"
        substitutionGroup = None
        def __init__(self, **kw):
            kw["pname"] = ("http://content.ws.lockss.org/","reactivateAuByIdResponse")
            kw["aname"] = "_reactivateAuByIdResponse"
            if ns0.reactivateAuByIdResponse_Def not in ns0.reactivateAuByIdResponse_Dec.__bases__:
                bases = list(ns0.reactivateAuByIdResponse_Dec.__bases__)
                bases.insert(0, ns0.reactivateAuByIdResponse_Def)
                ns0.reactivateAuByIdResponse_Dec.__bases__ = tuple(bases)

            ns0.reactivateAuByIdResponse_Def.__init__(self, **kw)
            if self.pyclass is not None: self.pyclass.__name__ = "reactivateAuByIdResponse_Dec_Holder"

    class reactivateAusByIdList_Dec(ElementDeclaration):
        literal = "reactivateAusByIdList"
        schema = "http://content.ws.lockss.org/"
        substitutionGroup = None
        def __init__(self, **kw):
            kw["pname"] = ("http://content.ws.lockss.org/","reactivateAusByIdList")
            kw["aname"] = "_reactivateAusByIdList"
            if ns0.reactivateAusByIdList_Def not in ns0.reactivateAusByIdList_Dec.__bases__:
                bases = list(ns0.reactivateAusByIdList_Dec.__bases__)
                bases.insert(0, ns0.reactivateAusByIdList_Def)
                ns0.reactivateAusByIdList_Dec.__bases__ = tuple(bases)

            ns0.reactivateAusByIdList_Def.__init__(self, **kw)
            if self.pyclass is not None: self.pyclass.__name__ = "reactivateAusByIdList_Dec_Holder"

    class reactivateAusByIdListResponse_Dec(ElementDeclaration):
        literal = "reactivateAusByIdListResponse"
        schema = "http://content.ws.lockss.org/"
        substitutionGroup = None
        def __init__(self, **kw):
            kw["pname"] = ("http://content.ws.lockss.org/","reactivateAusByIdListResponse")
            kw["aname"] = "_reactivateAusByIdListResponse"
            if ns0.reactivateAusByIdListResponse_Def not in ns0.reactivateAusByIdListResponse_Dec.__bases__:
                bases = list(ns0.reactivateAusByIdListResponse_Dec.__bases__)
                bases.insert(0, ns0.reactivateAusByIdListResponse_Def)
                ns0.reactivateAusByIdListResponse_Dec.__bases__ = tuple(bases)

            ns0.reactivateAusByIdListResponse_Def.__init__(self, **kw)
            if self.pyclass is not None: self.pyclass.__name__ = "reactivateAusByIdListResponse_Dec_Holder"

    class LockssWebServicesFault_Dec(ElementDeclaration):
        literal = "LockssWebServicesFault"
        schema = "http://content.ws.lockss.org/"
        substitutionGroup = None
        def __init__(self, **kw):
            kw["pname"] = ("http://content.ws.lockss.org/","LockssWebServicesFault")
            kw["aname"] = "_LockssWebServicesFault"
            if ns0.lockssWebServicesFaultInfo_Def not in ns0.LockssWebServicesFault_Dec.__bases__:
                bases = list(ns0.LockssWebServicesFault_Dec.__bases__)
                bases.insert(0, ns0.lockssWebServicesFaultInfo_Def)
                ns0.LockssWebServicesFault_Dec.__bases__ = tuple(bases)

            ns0.lockssWebServicesFaultInfo_Def.__init__(self, **kw)
            if self.pyclass is not None: self.pyclass.__name__ = "LockssWebServicesFault_Dec_Holder"

# end class ns0 (tns: http://content.ws.lockss.org/)
