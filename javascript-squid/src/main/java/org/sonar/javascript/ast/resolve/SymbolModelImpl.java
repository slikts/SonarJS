/*
 * SonarQube JavaScript Plugin
 * Copyright (C) 2011 SonarSource and Eriks Nukis
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.javascript.ast.resolve;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.sonar.api.config.Settings;
import org.sonar.api.source.Symbolizable;
import org.sonar.javascript.api.SymbolModelBuilder;
import org.sonar.javascript.ast.resolve.type.TypeVisitor;
import org.sonar.javascript.highlighter.SourceFileOffsets;
import org.sonar.plugins.javascript.api.symbols.Symbol;
import org.sonar.plugins.javascript.api.symbols.SymbolModel;
import org.sonar.plugins.javascript.api.tree.ScriptTree;
import org.sonar.plugins.javascript.api.tree.Tree;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SymbolModelImpl implements SymbolModel, SymbolModelBuilder {

  private Map<Symbol, Scope> symbolScope = Maps.newHashMap();
  private Set<Scope> scopes = Sets.newHashSet();
  private Scope globalScope;

  public static SymbolModelImpl create(ScriptTree script, @Nullable Symbolizable symbolizable, @Nullable SourceFileOffsets sourceFileOffsets, @Nullable Settings settings) {
    SymbolModelImpl symbolModel = new SymbolModelImpl();
    new SymbolVisitor(symbolModel, symbolizable, sourceFileOffsets).visitScript(script);
    new TypeVisitor(settings).visitScript(script);
    return symbolModel;
  }

  private void setScopeForSymbol(Symbol symbol, Scope scope) {
    symbolScope.put(symbol, scope);
  }

  @Override
  public Scope globalScope() {
    return globalScope;
  }

  @Override
  public void addScope(Scope scope){
    if (scopes.isEmpty()){
      globalScope = scope;
    }
    scopes.add(scope);
  }

  @Override
  public Set<Scope> getScopes(){
    return scopes;
  }

  @Override
  public Symbol declareSymbol(String name, Symbol.Kind kind, Scope scope) {
    Symbol symbol = scope.getSymbol(name);
    if (symbol == null) {
      symbol = new Symbol(name, kind, scope);
      scope.addSymbol(symbol);
      setScopeForSymbol(symbol, scope);
    }
    return symbol;
  }

  @Override
  public Symbol declareBuiltInSymbol(String name, Symbol.Kind kind, Scope scope) {
    Symbol symbol = scope.getSymbol(name);
    if (symbol == null) {
      symbol = new Symbol(name, kind, scope);
      symbol.setBuiltIn(true);
      scope.addSymbol(symbol);
      setScopeForSymbol(symbol, scope);
    }
    return symbol;
  }

  /**
   * Returns all symbols in script
   */
  @Override
  public Set<Symbol> getSymbols() {
    return symbolScope.keySet();
  }

  /**
   *
   * @param kind kind of symbols to look for
   * @return list of symbols with the given kind
   */
  @Override
  public Set<Symbol> getSymbols(Symbol.Kind kind) {
    Set<Symbol> result = new HashSet<>();
    for (Symbol symbol : getSymbols()){
      if (kind.equals(symbol.kind())){
        result.add(symbol);
      }
    }
    return result;
  }

  /**
   *
   * @param name name of symbols to look for
   * @return list of symbols with the given name
   */
  @Override
  public Set<Symbol> getSymbols(String name) {
    Set<Symbol> result = new HashSet<>();
    for (Symbol symbol : getSymbols()){
      if (name.equals(symbol.name())){
        result.add(symbol);
      }
    }
    return result;
  }

  @Nullable
  @Override
  public Scope getScope(Tree tree) {
    for (Scope scope : getScopes()){
      if (scope.tree().equals(tree)){
        return scope;
      }
    }
    return null;
  }

}
