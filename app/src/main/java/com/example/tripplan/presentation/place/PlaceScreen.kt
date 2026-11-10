package com.example.tripplan.presentation.place

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tripplan.domain.model.PlaceInput
import com.example.tripplan.domain.usecase.CreatePlaceUseCase
import com.example.tripplan.domain.usecase.DeletePlaceUseCase
import com.example.tripplan.domain.usecase.ObservePlacesUseCase
import com.example.tripplan.domain.usecase.UpdatePlaceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel class PlaceViewModel @Inject constructor(observe: ObservePlacesUseCase, private val create: CreatePlaceUseCase,
    private val update: UpdatePlaceUseCase, private val delete: DeletePlaceUseCase): ViewModel() {
    private val _state=MutableStateFlow(PlaceUiState()); val state=_state.asStateFlow()
    init { viewModelScope.launch { observe().collect { list -> _state.value=PlaceUiState(false, list.map { PlaceUiModel(it.id,it.name,it.address) }) } } }
    fun add(name:String,address:String){viewModelScope.launch{create(PlaceInput(name,address))}}
    fun save(id:Long?,name:String,address:String){viewModelScope.launch{if(id==null)create(PlaceInput(name,address)) else update(id,PlaceInput(name,address))}}
    fun delete(id:Long){viewModelScope.launch{delete.invoke(id)}}
}
data class PlaceUiModel(val id:Long,val name:String,val address:String)
data class PlaceUiState(val loading:Boolean=true,val places:List<PlaceUiModel> = emptyList())

@Composable fun PlaceScreen(onBack:()->Unit, viewModel:PlaceViewModel=hiltViewModel()) { val s by viewModel.state.collectAsState(); PlaceContent(s,onBack,viewModel::save,viewModel::delete) }
@Composable internal fun PlaceContent(state:PlaceUiState,onBack:()->Unit={},onSave:(Long?,String,String)->Unit={_,_,_->},onDelete:(Long)->Unit={}) {
    var name by remember{mutableStateOf("")}; var address by remember{mutableStateOf("")}; var editingId by remember{mutableStateOf<Long?>(null)}
    LazyColumn(Modifier.fillMaxSize().padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        item{Row{TextButton(onClick=onBack){Text("뒤로")};Text("저장된 장소",style=MaterialTheme.typography.headlineSmall)}}
        item{Column{OutlinedTextField(name,{name=it},Modifier.fillMaxWidth(),label={Text("장소 이름")});OutlinedTextField(address,{address=it},Modifier.fillMaxWidth(),label={Text("주소(선택)")});Button(onClick={if(name.isNotBlank()){onSave(editingId,name,address);name="";address="";editingId=null}}){Text(if(editingId==null)"장소 추가" else "장소 수정 저장")}}}
        if(!state.loading&&state.places.isEmpty()) item{Text("저장된 장소가 없습니다.")}
        items(state.places,key={it.id}){p->Row(Modifier.fillMaxWidth()){Column(Modifier.weight(1f)){Text(p.name);if(p.address.isNotBlank())Text(p.address)};TextButton(onClick={editingId=p.id;name=p.name;address=p.address}){Text("수정")};TextButton(onClick={onDelete(p.id)}){Text("삭제")}}}
    }
}
@Preview(showBackground=true) @Composable private fun PlacePreview(){MaterialTheme{PlaceContent(PlaceUiState(false,listOf(PlaceUiModel(1,"성산일출봉","제주"))))}}
